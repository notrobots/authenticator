package dev.notrobots.authenticator.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.notrobots.authenticator.extensions.contains
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.util.cloneObject
import dev.notrobots.authenticator.util.hashCodeOf
import dev.notrobots.authenticator.util.isValidBase32
import dev.notrobots.preferences2.util.parseEnum
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.*
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
    var accountId: Long = DEFAULT_ID

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
    var type: OTPType = DEFAULT_TYPE    //TODO: This should be a required field

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

//    var hotpGenerationDisabled: Boolean = false

    val path
        get() = if (label.isNotEmpty()) "$label:$name" else name
    val displayName
        get() = if (label.isNotEmpty()) "$label ($name)" else name

    constructor() : this("", "")

    public override fun clone(): Account {
        return cloneObject(this)
    }

    override fun equals(other: Any?): Boolean {
        return other is Account &&
               other.accountId == accountId &&
               other.name == name &&
               other.label == label &&
               other.issuer == issuer &&
               other.secret == secret &&
               other.type == type &&
               other.counter == counter &&
               other.digits == digits &&
               other.period == period &&
               other.algorithm == algorithm &&
               other.order == order
    }

    override fun hashCode(): Int {
        return hashCodeOf(
            name,
            secret,
            accountId,
            issuer,
            label,
            type,
            counter,
            digits,
            period,
            algorithm,
            order
        )
    }

    companion object : JsonSerializable<Account>, UriSerializable<Account> {
        const val DEFAULT_ORDER = -1L
        const val DEFAULT_ID = 0L
        val DEFAULT_TYPE = OTPType.TOTP
        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD = 30L
        const val DEFAULT_COUNTER = 0L
        val DEFAULT_ALGORITHM = HmacAlgorithm.SHA1
        const val URI_SCHEME = "otpauth"
        const val TAGS = "tags"
        const val NAME = "name"
        const val LABEL = "label"
        const val SECRET = "secret"
        const val ISSUER = "issuer"
        const val COUNTER = "counter"
        const val TYPE = "type"
        const val ALGORITHM = "algorithm"
        const val DIGITS = "digits"
        const val PERIOD = "period"
        const val ORDER = "order"
        val HOTP_CODE_INTERVAL = TimeUnit.SECONDS.toMillis(10)  //TODO Move to App
        private val pathError = { throw Exception("Path malformed, must be /label:name or /name") }
        private val typeError = { throw Exception("Type must be one of [${OTPType.values().joinToString()}]") }

        override fun toJson(value: Account): JSONObject {
            return toJson(value, emptyList())
        }

        fun toJson(value: Account, tags: List<Tag>): JSONObject {
            val json = JSONObject()

            json.put(NAME, value.name)
            json.put(LABEL, value.label)
            json.put(SECRET, value.secret)
            json.put(ISSUER, value.issuer)
            json.put(COUNTER, value.counter)
            json.put(TYPE, value.type.toString())
            json.put(ALGORITHM, value.algorithm.toString())
            json.put(DIGITS, value.digits)
            json.put(PERIOD, value.period)
            json.put(ORDER, value.order)
            json.put(TAGS, JSONArray(tags.map(Tag::name)))

            return json
        }

        override fun fromJson(json: JSONObject): Account {
            require(json.has(NAME)) {
                "Required field '$NAME' is missing"
            }

            require(json.has(SECRET)) {
                "Required field '$SECRET' is missing"
            }

            require(json.has(TYPE)) {
                "Required field '$TYPE' is missing"
            }

            return Account(
                json.getString(NAME),
                json.getString(SECRET)
            ).apply {
                type = if (json.has(TYPE)) {
                    parseEnum(json.getString(TYPE), true)
                } else DEFAULT_TYPE

                label = if (json.has(LABEL)) {
                    json.getString(LABEL)
                } else ""

                issuer = if (json.has(ISSUER)) {
                    json.getString(ISSUER)
                } else ""

                counter = if (json.has(COUNTER)) {
                    json.getLong(COUNTER)
                } else DEFAULT_COUNTER

                algorithm = if (json.has(ALGORITHM)) {
                    parseEnum(json.getString(ALGORITHM), true)
                } else DEFAULT_ALGORITHM

                digits = if (json.has(DIGITS)) {
                    json.getInt(DIGITS)
                } else DEFAULT_DIGITS

                period = if (json.has(PERIOD)) {
                    json.getLong(PERIOD)
                } else DEFAULT_PERIOD

                order = if (json.has(ORDER)) {
                    json.getLong(ORDER)
                } else DEFAULT_ORDER
            }
        }

        fun tagsFromJson(json: JSONObject): Set<String> {
            require(json.has(TAGS)) {
                "Missing '$TAGS' field"
            }

            val tagsArray = json.getJSONArray(TAGS)
            val tags = mutableSetOf<String>()

            for (i in 0 until tagsArray.length()) {
                val tagName = tagsArray[i]

                require(tagName is String) {
                    "Tag list must be only contain strings"
                }

                tags.add(tagName)
            }

            return tags
        }

        override fun toUri(value: Account): Uri {
            return toUri(value, emptyList())
        }

        fun toUri(value: Account, tags: List<Tag>): Uri {
            val uri = Uri.Builder()

            uri.scheme(URI_SCHEME)
            uri.authority(value.type.toString().lowercase())
            uri.path(value.path)
            uri.appendQueryParameter(SECRET, value.secret)

            if (value.issuer.isNotBlank()) {
                uri.appendQueryParameter(ISSUER, value.issuer)
            }

            uri.appendQueryParameter(COUNTER, value.counter.toString())
            uri.appendQueryParameter(DIGITS, value.digits.toString())
            uri.appendQueryParameter(PERIOD, value.period.toString())
            uri.appendQueryParameter(ALGORITHM, value.algorithm.toString().lowercase())

            if (tags.isNotEmpty()) {
                uri.appendQueryParameter(TAGS, tags.joinToString(",") { it.name })
            }

            return uri.build()
        }

        override fun fromUri(uri: Uri): Account {
            //
            // Optional fields
            //
            val algorithm = if (ALGORITHM in uri) {
                parseEnum(uri[ALGORITHM], true)
            } else {
                DEFAULT_ALGORITHM
            }
            val digits = if (DIGITS in uri) {
                uri[DIGITS]?.toIntOrNull() ?: throw Exception("Digits number must be an integer")   //TODO: It should say "number"
            }    //TODO: Between x and y
            else {
                DEFAULT_DIGITS
            }
            val counter = if (COUNTER in uri) {
                uri[COUNTER]?.toLongOrNull() ?: throw Exception("Counter value must be a number")
            } else {
                DEFAULT_COUNTER
            }
            val period = if (PERIOD in uri) {
                uri[PERIOD]?.toLongOrNull() ?: throw Exception("Period must be a number")
            } else {
                DEFAULT_PERIOD
            }
            val issuer = if (ISSUER in uri) uri[ISSUER]!! else ""

            //
            // Required fields
            //
            val type = parseEnum<OTPType>(uri.authority?.toUpperCase(Locale.getDefault())) ?: typeError()
            val path = parsePath(uri.path)
            val name = path.groupValues[2]
            val label = path.groupValues[1]
            val secret = uri[SECRET] ?: throw Exception("Missing parameter 'secret'")

            if (name.isBlank()) {
                throw Exception("Name cannot be empty")
            }

            if (label.isOnlySpaces()) {
                throw Exception("Label cannot be blank")
            }

            if (issuer.isOnlySpaces()) {
                throw Exception("Issuer cannot be blank")
            }

            if (secret.isBlank()) {
                throw Exception("Secret cannot be empty")
            }

            if (!isValidBase32(secret)) {
                throw Exception("Secret key must be a base32 string")
            }

            return Account(name, secret).apply {
                this.issuer = issuer
                this.label = label
                this.type = type
                this.algorithm = algorithm
                this.digits = digits
                this.counter = counter
                this.period = period
            }
        }

        fun tagsFromUri(uri: Uri): Set<String> {
            val tagList = uri.getQueryParameter(TAGS)

            require(tagList != null && tagList.isNotBlank()) {
                "${URI_SCHEME}://type/?tags=[EMPTY]"
            }

            return tagList.split(",").toSet()
        }

        /**
         * Returns name and label for the given [path].
         */
        fun parsePath(path: String?): MatchResult {
            return path?.removePrefix("/").let {
                if (it == null || it.isBlank()) {
                    pathError()
                }

                Regex("^(?:(.+):)?(.+)$").find(it) ?: pathError()
            }
        }
    }
}