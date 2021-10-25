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
//     * Whether or not the secret is a base32 string
//     */
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

        uri.scheme(AccountExporter.OTP_SCHEME)
        uri.authority(type.toString().toLowerCase())
        uri.path(path)
        uri.appendQueryParameter(AccountExporter.OTP_SECRET, secret)
        uri.appendQueryParameter(AccountExporter.OTP_COUNTER, counter.toString())

        if (issuer.isNotBlank()) {
            uri.appendQueryParameter(AccountExporter.OTP_ISSUER, issuer)
        }

        return uri.build()
    }

    companion object {
        const val DEFAULT_GROUP_ID = 1L
        val HOTP_CODE_INTERVAL = TimeUnit.SECONDS.toMillis(10)
    }
}