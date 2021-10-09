package dev.notrobots.authenticator.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.proto.GoogleAuthenticator
import java.io.Serializable
import dev.notrobots.authenticator.util.isValidBase32

@Entity
data class Account(
    /**
     * Name associated with this account
     */
    var name: String,
    /**
     * Account secret, should be a base32 string
     */
    var secret: String,
) : Serializable {
    /**
     * Primary key
     */
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null    //FIXME: This should be 0 and non-nullable

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
     * Position of this account in the account list
     */
    var order: Long = -1

    /**
     * Whether or not the secret is a base32 string
     */
//    var isBase32: Boolean = true

    /**
     * Whether or not this item is selected
     */
    @Ignore
    var isSelected: Boolean = false

    val path
        get() = if (label.isNotEmpty()) "$label:$name" else name
    val displayName
        get() = if (label.isNotEmpty()) "$label ($name)" else name

    constructor() : this("", "")

    fun toggleSelected() {
        isSelected = !isSelected
    }

    fun getUri(): Uri {
        val uri = Uri.Builder()

        uri.scheme(OTP_SCHEME)
        uri.authority(type.toString().toLowerCase())
        uri.path(path)
        uri.appendQueryParameter(OTP_SECRET, secret)

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

                if (name.isBlank()) {
                    error("Name cannot be empty")
                }

                validateSecret(secret)

                //
                // Optional fields
                //
                val label = path.groupValues[1]

                if (label.isOnlySpaces()) {
                    error("Label cannot be blank")
                }

                val issuer = uri[OTP_ISSUER] ?: ""

                if (issuer.isOnlySpaces()) {
                    error("Issuer cannot be blank")
                }

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
         * Validates the given [Account] fields and throws an exception if any of them doesn't
         * follow the requirements
         */
        fun validateSecret(secret: String) {
            if (secret.isBlank()) {
                error("Secret cannot be empty")
            }

            //TODO: If isBase32Secret is true then check if it's actually a base32 string and pass it to the GoogleAuthenticator
            // if isBase32Secret is false then pass the string to the TotpGenerator

            //TODO:FEATURE It would be nice to show the users an advanced error and a regular error
            if (!isValidBase32(secret)) {   //&& isBase32Secret
                error("Secret key must be a base32 string")
            }

            // This check shouldn't be need but you never know
            if (!OTPProvider.checkSecret(secret)) {
                error("Invalid secret key")
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