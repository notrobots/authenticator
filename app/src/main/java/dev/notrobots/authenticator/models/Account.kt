package dev.notrobots.authenticator.models

import androidx.room.Entity
import androidx.room.Index
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import java.io.Serializable
import java.util.concurrent.TimeUnit

@Entity(indices = [Index(value = ["issuer", "name", "label"], unique = true)])
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
    var type: OTPType = DEFAULT_TYPE

    /**
     * Counter used by the HOTP
     */
    var counter: Long = DEFAULT_COUNTER

    /**
     * Length of the generated OTP pin
     */
    var digits: Int = DEFAULT_DIGITS

    /**
     * Time after the pin is generated for this account
     */
    var period: Long = DEFAULT_PERIOD

    /**
     * Algorithm used to generate the pin
     */
    var algorithm: HmacAlgorithm = DEFAULT_ALGORITHM

    /**
     * ID of the group this account belongs to
     */
    var groupId: Long = DEFAULT_GROUP_ID

    val path
        get() = if (label.isNotEmpty()) "$label:$name" else name
    val displayName
        get() = if (label.isNotEmpty()) "$label ($name)" else name

    constructor() : this("", "")

    public override fun clone(): Account {
        return super.clone() as Account
    }

    companion object {
        val DEFAULT_TYPE = OTPType.TOTP
        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD = 30L
        const val DEFAULT_COUNTER = 0L
        val DEFAULT_ALGORITHM = HmacAlgorithm.SHA1
        const val DEFAULT_GROUP_ID = 1L
        val HOTP_CODE_INTERVAL = TimeUnit.SECONDS.toMillis(10)
    }
}