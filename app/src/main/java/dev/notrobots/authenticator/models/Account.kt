package dev.notrobots.authenticator.models

import android.net.Uri
import androidx.room.Entity
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import java.io.Serializable
import dev.notrobots.authenticator.util.isValidBase32
import java.util.concurrent.TimeUnit

@Entity
class Account(
    name: String,
    /**
     * Account secret, should be a base32 string
     */
    var secret: String,
) : BaseAccount(name), Serializable, Cloneable {
    /**
     * Account issuer, should be the company's website
     */
    var issuer: String = ""

    /**
     * Additional naming for the account, should be the company's name
     */
    var label: String = ""

    /**
     * OTP type
     */
    var type: OTPType = OTPType.TOTP

    /**
     * Counter used by the HOTP
     */
    var counter: Long = 0

    /**
     * ID of the group this account belongs to
     */
    var groupId: Long = DEFAULT_GROUP_ID

//    /**
//     * Parent group of this account, null if the account doesn't belong to any group
//     */
//    var group: AccountGroup? = null

    /**
     * Whether or not the secret is a base32 string
     */
//    var isBase32: Boolean = true

    val path
        get() = if (label.isNotEmpty()) "$label:$name" else name
    val displayName
        get() = if (label.isNotEmpty()) "$label ($name)" else name

    constructor() : this("", "")

    public override fun clone(): Account {
        return super.clone() as Account
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
                other is Account &&
                secret == other.secret &&
                issuer == other.issuer &&
                label == other.label &&
                type == other.type &&
                groupId == other.groupId
    }

    fun getUri(): Uri {
        val uri = Uri.Builder()

        uri.scheme(OTP_SCHEME)
        uri.authority(type.toString().toLowerCase())
        uri.path(path)
        uri.appendQueryParameter(OTP_SECRET, secret)
        uri.appendQueryParameter(OTP_COUNTER, counter.toString())

        if (issuer.isNotBlank()) {
            uri.appendQueryParameter(OTP_ISSUER, issuer)
        }

        return uri.build()
    }

    companion object {
        private const val OTP_SCHEME = "otpauth"
        private const val OTP_SECRET = "secret"
        private const val OTP_ISSUER = "issuer"
        private const val OTP_COUNTER = "counter"
        private const val OTP_ALGORITHM = "algorithm"
        private const val OTP_DIGITS = "digits"
        private const val OTP_PERIOD = "period"
        private const val DEFAULT_OTP_DIGITS = 6
        private const val DEFAULT_OTP_PERIOD = 30
        private const val DEFAULT_OTP_COUNTER = 0
        private val DEFAULT_OTP_ALGORITHM = OTPAlgorithm.SHA1
        const val DEFAULT_GROUP_ID = 1L
        val HOTP_CODE_INTERVAL = TimeUnit.SECONDS.toMillis(10)

        fun parse(uri: String): Account {
            return parse(Uri.parse(uri))
        }

        /**
         * Parses the given [uri] into an [Account] object.
         *
         * The [Uri] must match the format:
         *
         * + `otpauth://{type}/{label}:{name}?secret={secret}`
         *
         * Where `label` can be optional and `name`, `type` and `secret` must be defined. In addition
         * more values can be specified:
         * + issuer, can be empty but not blank
         */
        fun parse(uri: Uri): Account {
            val typeError = { error("Type must be one of [${OTPType.values().joinToString()}]") }

            if (uri.scheme == OTP_SCHEME) {
                //
                // Required fields
                //
                val type = parseEnum<OTPType>(uri.authority?.toUpperCase()) ?: typeError()
                val path = parsePath(uri)
                val name = path.groupValues[2]
                val secret = uri[OTP_SECRET] ?: error("Missing parameter 'secret'")

                AccountExporter.validateName(name)
                AccountExporter.validateSecret(secret)

                //
                // Optional fields
                //
                val label = path.groupValues[1]
                val issuer = uri[OTP_ISSUER] ?: ""

                AccountExporter.validateLabel(label)
                AccountExporter.validateIssuer(issuer)

                //
                // Extra optional fields
                //
//                val algorithm = parseEnum(uri[OTP_ALGORITHM], true) ?: DEFAULT_OTP_ALGORITHM
//                val digits = uri[OTP_DIGITS]?.toIntOrNull() ?: DEFAULT_OTP_DIGITS
//                val counter = uri[OTP_COUNTER]?.toIntOrNull() ?: DEFAULT_OTP_COUNTER
//                val period = uri[OTP_PERIOD]?.toIntOrNull() ?: DEFAULT_OTP_PERIOD
//                val isBase32 = uri[OTP_BASE32]?.toBoolean() ?: DEFAULT_BASE32

                return Account(name, secret).apply {
                    this.issuer = issuer
                    this.label = label
                    this.type = type
                }
            } else {
                error("Scheme should be 'otpauth'")
            }
        }

        /**
         * Parses the path and returns a [MatchResult]
         */
        private fun parsePath(uri: Uri): MatchResult {
            val pathError = { error("Path malformed, must be /label:name or /name") }
            val path = uri.path.let {
                val s = it?.removePrefix("/")

                if (s.isNullOrBlank()) pathError() else s
            }

            return Regex("^(?:(.+):)?(.+)$").find(path) ?: pathError()
        }
    }
}