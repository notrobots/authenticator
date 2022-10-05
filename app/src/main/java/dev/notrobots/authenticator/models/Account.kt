package dev.notrobots.authenticator.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import java.io.Serializable
import java.util.concurrent.TimeUnit

@Entity(indices = [Index(value = ["issuer", "name", "label"], unique = true)])
data class Account(
    /**
     * Displayed name for this account
     */
    var name: String,
    /**
     * Account secret, should be a base32 string
     */
    var secret: String,
) : Serializable, Cloneable {
    /**
     * Room Id for this item
     */
    @PrimaryKey(autoGenerate = true)
    var id: Long = DEFAULT_ID

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
     * Position of this item in the list
     *
     * The default value is -1, which puts the item at the end of the list
     */
    var order: Long = DEFAULT_ORDER

//    var forcedClearText: Boolean = false
//
//    var hotpGenerationDisabled: Boolean = false

    val path
        get() = if (label.isNotEmpty()) "$label:$name" else name
    val displayName
        get() = if (label.isNotEmpty()) "$label ($name)" else name

    constructor() : this("", "")

    public override fun clone(): Account {
        return super.clone() as Account
    }

    override fun equals(other: Any?): Boolean {
        return other is Account &&
               other.name == name &&
               other.label == label &&
               other.issuer == issuer &&
               other.secret == secret &&
               other.id == id &&
               other.type == type &&
               other.counter == counter &&
               other.digits == digits &&
               other.period == period &&
               other.algorithm == algorithm &&
               other.order == order
    }

    companion object {
        const val DEFAULT_ORDER = 0L
        const val DEFAULT_ID = 0L
        val DEFAULT_TYPE = OTPType.TOTP
        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD = 30L
        const val DEFAULT_COUNTER = 0L
        val DEFAULT_ALGORITHM = HmacAlgorithm.SHA1
        val HOTP_CODE_INTERVAL = TimeUnit.SECONDS.toMillis(10)
    }
}