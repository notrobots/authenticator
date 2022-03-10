package dev.notrobots.authenticator.util

import android.net.Uri
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.extensions.contains
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.models.QRCode
import dev.notrobots.authenticator.proto.Authenticator.*
import dev.notrobots.authenticator.proto.GoogleAuthenticator.*
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import java.io.Serializable
import java.util.*

object AccountExporter {
    //TODO: Let the user chose the resolution 264, 512, 1024, 2048
    const val QR_BITMAP_SIZE = 512

    //TODO: There should be a list of sizes: 64, 128, 256, 512
    const val QR_MAX_BYTES = 512       // Max: 2953
    const val BACKUP_OTP_SCHEME = "otpauth"
    const val BACKUP_OTP_AUTHORITY = "backup"
    const val BACKUP_OTP_DATA = "data"
    const val BACKUP_OTP_SCHEME_GOOGLE = "otpauth-migration"
    const val BACKUP_OTP_AUTHORITY_GOOGLE = "offline"
    const val BACKUP_OTP_DATA_GOOGLE = "data"
    const val OTP_SCHEME = "otpauth"
    const val OTP_SECRET = "secret"
    const val OTP_ISSUER = "issuer"
    const val OTP_COUNTER = "counter"
    const val OTP_ALGORITHM = "algorithm"
    const val OTP_DIGITS = "digits"
    const val OTP_PERIOD = "period"
    const val OTP_ORDER = "order"

    fun exportUris(accounts: List<Account>): List<Uri> {
        return accounts.map(AccountExporter::parseUri)
    }

    fun exportText(accounts: List<Account>): String {
        return exportProtobuf(accounts, QR_MAX_BYTES).joinToString("\n")
    }

    fun exportQR(accounts: List<Account>): List<QRCode> {
        return exportProtobuf(accounts).map { QRCode(it.toString(), QR_BITMAP_SIZE) }
    }

    fun exportPlainText(accounts: List<Account>): String {
        return exportUris(accounts).joinToString("\n")
    }

    fun exportPlainQR(accounts: List<Account>): List<QRCode> {
        return exportUris(accounts).map { QRCode(it.toString(), QR_BITMAP_SIZE) }
    }

    fun exportJson(accounts: List<Account>, tags: List<Any>): JSONObject {
//        val obj = JSONObject()
//        val accountArray = JSONArray().apply {
//            obj.put("accounts", this)
//        }
//        val tagArray = JSONArray().apply {
//            obj.put("tags", this)
//        }
//
//        return obj

        TODO()
    }

    fun exportGoogleAuthenticator(accounts: List<Account>): List<QRCode> {
        TODO()
    }

    fun exportMicrosoftAuthenticator(accounts: List<Account>): String {
        TODO()
    }

    fun import(json: JSONObject): ImportedData {
        TODO()
    }

    fun import(text: String): ImportedData {
        try {
            return import(JSONObject(text))
        } catch (e: Exception) {
        }

        val uris = text.split("\n").map {
            Uri.parse(it)
        }

        return import(uris)
    }

    fun import(uris: List<Uri>): ImportedData {
        val dataList = uris.map { import(it) }

        return ImportedData().apply {
            for (data in dataList) {
                accounts.addAll(data.accounts)
            }
        }
    }

    /**
     * Imports the given [uri].
     */
    fun import(uri: Uri): ImportedData {
        val importData = ImportedData()

        when (getUriType(uri)) {
            UriType.Plain -> importData.accounts.add(parseAccount(uri))
            UriType.Backup -> {
                val data = uri.getQueryParameter(BACKUP_OTP_DATA)

                if (data.isNullOrEmpty()) {
                    throw Exception("Data parameter is empty")
                } else {
                    val payload = MigrationPayload.parseFrom(decodeMessage(data))

                    for (account in payload.accountsList) {
                        importData.accounts.add(
                            Account(
                                account.name,
                                decodeSecret(account.secret)
                            ).apply {
                                order = account.order
                                issuer = account.issuer
                                label = account.label
                                type = decodeOTPType(account.type)
                                counter = account.counter
                                digits = account.digits
                                period = account.period
                                algorithm = decodeAlgorithm(account.algorithm)
                            }
                        )
                    }
                }
            }

            else -> throw Exception("Unknown format")
        }

        return importData
    }

    /**
     * Parses the given [uriString] into an [Account].
     */
    fun parseAccount(uriString: String): Account {
        return parseAccount(Uri.parse(uriString))
    }

    /**
     * Parses the given [uri] into an [Account].
     */
    fun parseAccount(uri: Uri): Account {
        val pathError = { throw Exception("Path malformed, must be /label:name or /name") }
        val typeError = { throw Exception("Type must be one of [${OTPType.values().joinToString()}]") }

        //
        // Optional fields
        //
        val algorithm = if (OTP_ALGORITHM in uri) {
            parseEnum(uri[OTP_ALGORITHM], true) ?: throw Exception("Unknown algorithm")
        } else {
            Account.DEFAULT_ALGORITHM
        }
        val digits = if (OTP_DIGITS in uri) {
            uri[OTP_DIGITS]?.toIntOrNull() ?: throw Exception("Digits number must be an integer")
        }    //TODO: Between x and y
        else {
            Account.DEFAULT_DIGITS
        }
        val counter = if (OTP_COUNTER in uri) {
            uri[OTP_COUNTER]?.toLongOrNull() ?: throw Exception("Counter value must be an integer")
        } else {
            Account.DEFAULT_COUNTER
        }
        val period = if (OTP_PERIOD in uri) {
            uri[OTP_PERIOD]?.toLongOrNull() ?: throw Exception("Period must be an integer")
        } else {
            Account.DEFAULT_PERIOD
        }
        val issuer = if (OTP_ISSUER in uri) uri[OTP_ISSUER]!! else ""

        //
        // Required fields
        //
        val type = parseEnum<OTPType>(uri.authority?.toUpperCase(Locale.getDefault())) ?: typeError()
        val path = uri.path?.removePrefix("/").let {
            if (it == null || it.isBlank()) {
                pathError()
            }

            Regex("^(?:(.+):)?(.+)$").find(it) ?: pathError()
        }
        val name = path.groupValues[2]
        val label = path.groupValues[1]
        val secret = uri[OTP_SECRET] ?: throw Exception("Missing parameter 'secret'")

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

    /**
     * Parses the given [account] into an [Uri].
     */
    fun parseUri(account: Account): Uri {
        val uri = Uri.Builder()

        uri.scheme(OTP_SCHEME)
        uri.authority(account.type.toString().toLowerCase(Locale.getDefault()))
        uri.path(account.path)
        uri.appendQueryParameter(OTP_SECRET, account.secret)

        if (account.issuer.isNotBlank()) {
            uri.appendQueryParameter(OTP_ISSUER, account.issuer)
        }

        uri.appendQueryParameter(OTP_COUNTER, account.counter.toString())
        uri.appendQueryParameter(OTP_DIGITS, account.digits.toString())
        uri.appendQueryParameter(OTP_PERIOD, account.period.toString())
        uri.appendQueryParameter(OTP_ALGORITHM, account.algorithm.toString().toLowerCase(Locale.getDefault()))

        return uri.build()
    }

    /**
     * Returns the [UriType] of the given [uri].
     */
    fun getUriType(uri: Uri): UriType {
        val scheme = uri.scheme?.lowercase()
        val authority = uri.authority?.lowercase()

        return when {
            scheme == BACKUP_OTP_SCHEME_GOOGLE && authority == BACKUP_OTP_AUTHORITY_GOOGLE -> UriType.GoogleAuthenticatorBackup
            scheme == OTP_SCHEME -> when (authority) {
                in OTPType.stringValues() -> UriType.Plain
                BACKUP_OTP_AUTHORITY -> UriType.Backup

                else -> UriType.Unknown
            }

            else -> UriType.Unknown
        }
    }

    /**
     * Returns whether or not the given [uri] is a backup.
     */
    fun isBackup(uri: Uri): Boolean {
        val type = getUriType(uri)

        return type == UriType.Backup || type == UriType.GoogleAuthenticatorBackup
    }

    /**
     * Returns whether or not the given [data] is a backup.
     */
    fun isBackup(data: String): Boolean {
        try {
            JSONObject(data)
            return true
        } catch (e: Exception) {
        }

        try {
            return isBackup(Uri.parse(data))
        } catch (e: Exception) {
        }

        return false
    }

    private fun exportProtobuf(accounts: List<Account>, maxBytes: Int = 0): List<Uri> {
        val chunk = mutableSetOf<MessageLite>()
        var chunkSize = 0
        val uris = mutableListOf<Uri>()
        val serializeAccount = { it: Account ->
            MigrationPayload.Account.newBuilder()
                .setName(it.name)
                .setOrder(it.order)
                .setSecret(encodeSecret(it.secret))
                .setIssuer(it.issuer)
                .setLabel(it.label)
                .setType(encodeOTPType(it.type))
                .setCounter(it.counter)
                .setDigits(it.digits)
                .setPeriod(it.period)
                .setAlgorithm(encodeAlgorithm(it.algorithm))
                .build()
        }
        val serializeChunk = {
            val payload = MigrationPayload.newBuilder()

            for (message in chunk) {
                when (message) {
                    is MigrationPayload.Account -> payload.addAccounts(message)
                }
            }

            uris += Uri.Builder()
                .scheme(BACKUP_OTP_SCHEME)
                .authority(BACKUP_OTP_AUTHORITY)
                .appendQueryParameter(BACKUP_OTP_DATA, encodeMessage(payload.build()))
                .build()
        }

        if (maxBytes <= 0) {
            for (account in accounts) {
                chunk.add(serializeAccount(account))
            }
            serializeChunk()
        } else {
            //TODO: Try and eventually fix this
            for (cursor in accounts.indices) {
                val account = serializeAccount(accounts[cursor])
                var tmpSize = getMessageLength(account)

                if (tmpSize + chunkSize > maxBytes) {
                    serializeChunk()
                    chunk.clear()
                    chunkSize = 0
                }

                chunk.add(account)
                chunkSize += getMessageLength(account)
            }

            serializeChunk()
        }

        return uris
    }

    private fun getMessageLength(messageLite: MessageLite): Int {
        return 4 * (messageLite.serializedSize / 3)
    }

    private inline fun <reified E : Enum<E>> encodeAlgorithm(algorithm: HmacAlgorithm): E {
        return when (E::class) {
            GoogleMigrationPayload.Algorithm::class -> when (algorithm) {
                HmacAlgorithm.SHA1 -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA1
                HmacAlgorithm.SHA256 -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA256
                HmacAlgorithm.SHA512 -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA512

                else -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA1
            }

            MigrationPayload.Algorithm::class -> when (algorithm) {
                HmacAlgorithm.SHA1 -> MigrationPayload.Algorithm.ALGORITHM_SHA1
                HmacAlgorithm.SHA256 -> MigrationPayload.Algorithm.ALGORITHM_SHA256
                HmacAlgorithm.SHA512 -> MigrationPayload.Algorithm.ALGORITHM_SHA512

                else -> MigrationPayload.Algorithm.ALGORITHM_SHA1
            }

            else -> throw Exception("Unknown OTPType class")
        } as E
    }

    private inline fun <reified E : Enum<E>> decodeAlgorithm(typeValue: E): HmacAlgorithm {
        return when (E::class) {
            GoogleMigrationPayload.Algorithm::class -> when (typeValue) {
                GoogleMigrationPayload.Algorithm.ALGORITHM_SHA1 -> HmacAlgorithm.SHA1
                GoogleMigrationPayload.Algorithm.ALGORITHM_SHA256 -> HmacAlgorithm.SHA256
                GoogleMigrationPayload.Algorithm.ALGORITHM_SHA512 -> HmacAlgorithm.SHA512

                else -> HmacAlgorithm.SHA1
            }

            MigrationPayload.Algorithm::class -> when (typeValue) {
                MigrationPayload.Algorithm.ALGORITHM_SHA1 -> HmacAlgorithm.SHA1
                MigrationPayload.Algorithm.ALGORITHM_SHA256 -> HmacAlgorithm.SHA256
                MigrationPayload.Algorithm.ALGORITHM_SHA512 -> HmacAlgorithm.SHA512

                else -> HmacAlgorithm.SHA1
            }

            else -> throw Exception("Unknown OTPType class")
        }
    }

    private fun encodeSecret(secret: String): ByteString {
        val bytes = Base32().decode(secret)

        return ByteString.copyFrom(bytes)
    }

    private fun decodeSecret(byteString: ByteString): String {
        val bytes = Base32().encode(byteString.toByteArray())

        return bytes.toString(Charsets.UTF_8)
    }

    private inline fun <reified E : Enum<E>> encodeOTPType(otpType: OTPType): E {
        return when (E::class) {
            GoogleMigrationPayload.OTPType::class -> when (otpType) {
                OTPType.TOTP -> GoogleMigrationPayload.OTPType.OTP_TYPE_TOTP
                OTPType.HOTP -> GoogleMigrationPayload.OTPType.OTP_TYPE_HOTP
            }

            MigrationPayload.OTPType::class -> when (otpType) {
                OTPType.TOTP -> MigrationPayload.OTPType.OTP_TYPE_TOTP
                OTPType.HOTP -> MigrationPayload.OTPType.OTP_TYPE_HOTP
            }

            else -> throw Exception("Unknown OTPType class")
        } as E
    }

    private inline fun <reified E : Enum<E>> decodeOTPType(typeValue: E): OTPType {
        return when (E::class) {
            GoogleMigrationPayload.OTPType::class -> when (typeValue) {
                GoogleMigrationPayload.OTPType.OTP_TYPE_TOTP -> OTPType.TOTP
                GoogleMigrationPayload.OTPType.OTP_TYPE_HOTP -> OTPType.HOTP

                else -> OTPType.TOTP
            }

            MigrationPayload.OTPType::class -> when (typeValue) {
                MigrationPayload.OTPType.OTP_TYPE_TOTP -> OTPType.TOTP
                MigrationPayload.OTPType.OTP_TYPE_HOTP -> OTPType.HOTP

                else -> OTPType.TOTP
            }

            else -> throw Exception("Unknown OTPType class")
        }
    }

    private fun encodeMessage(message: MessageLite): String {
        val base64 = Base64().encode(message.toByteArray())
        val string = base64.toString(Charsets.UTF_8)

        return Uri.encode(string)
    }

    private fun decodeMessage(string: String): ByteArray {
        val base64 = Uri.decode(string).toByteArray(Charsets.UTF_8)

        return Base64().decode(base64)
    }

    data class ImportedData(
        val accounts: MutableList<Account> = mutableListOf(),
//        val tags: List<Tag> = emptyList()
    ) : Serializable

    enum class UriType {
        Unknown,
        Plain,
        Backup,
        GoogleAuthenticatorBackup
    }
}