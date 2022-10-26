package dev.notrobots.authenticator.util

import android.net.Uri
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.extensions.contains
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.models.ExportFormat
import dev.notrobots.authenticator.models.QRCode
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
        return accounts.map(::parseUri)
    }

    @Deprecated("Use BackupFormat.QR or BackupFormat.PlainText instead")
    fun exportText(accounts: List<Account>): String {
        return ProtobufUtil.serializePayload(
            accounts,
            ProtobufUtil.Variant.Default
        )[0].toString()
    }

    fun exportQR(accounts: List<Account>): List<QRCode> {
        return ProtobufUtil.serializePayload(
            accounts,
            ProtobufUtil.Variant.Default,
            QR_MAX_BYTES
        ).map { QRCode(it, QR_BITMAP_SIZE) }
    }

    fun exportPlainText(accounts: List<Account>): String {
        return exportUris(accounts).joinToString("\n")
    }

    fun exportPlainQR(accounts: List<Account>): List<QRCode> {
        return exportUris(accounts).map {
            QRCode(it, QR_BITMAP_SIZE)
        }
    }

    fun exportJson(accounts: List<Account>): JSONObject {
        return JsonUtil.serialize(accounts)
    }

//    fun exportGoogleAuthenticator(accounts: List<Account>): List<QRCode> {
//        TODO()
//    }

//    fun exportMicrosoftAuthenticator(accounts: List<Account>): String {
//        TODO()
//    }

    /**
     * Exports the given [accounts] using the given [format].
     */
    fun export(accounts: List<Account>, format: ExportFormat): Any {
        return when (format) {
            ExportFormat.Text -> exportText(accounts)
            ExportFormat.QR -> exportQR(accounts)
            ExportFormat.PlainText -> exportPlainText(accounts)
            ExportFormat.PlaintQR -> exportPlainQR(accounts)
            ExportFormat.Json -> exportJson(accounts)
//            BackupFormat.GoogleAuthenticator -> exportGoogleAuthenticator(accounts)

            else -> TODO()
        }
    }

    /**
     * Imports the given [json] object.
     *
     * The JSON object must follow [ExportFormat.Json]'s format
     */
    fun import(json: JSONObject): ImportedData {
        return ImportedData(
            JsonUtil.deserializeAccounts(json)
        )
    }

    /**
     * Imports the given [text].
     *
     * Used by:
     * + [ExportFormat.PlainText]
     * + [ExportFormat.PlaintQR]
     * + [ExportFormat.QR]
     * + [ExportFormat.GoogleAuthenticator]
     * + [ExportFormat.Json]
     */
    fun import(text: String): ImportedData {
        try {
            return import(JSONObject(text))
        } catch (e: Exception) {
        }

        val uris = text.lines().map {
            Uri.parse(it.trim())
        }

        return import(uris)
    }

    /**
     * Imports the given [uris].
     *
     * Used by:
     * + [ExportFormat.PlainText]
     * + [ExportFormat.PlaintQR]
     * + [ExportFormat.QR]
     * + [ExportFormat.GoogleAuthenticator]
     */
    fun import(uris: List<Uri>): ImportedData {
        val accounts = uris.flatMap {
            when (getUriType(it)) {
                UriType.Plain -> listOf(parseAccount(it))
                UriType.Backup -> {
                    val data = it.getQueryParameter(BACKUP_OTP_DATA)

                    if (data.isNullOrEmpty()) {
                        throw Exception("Data parameter is empty")
                    } else {
                        ProtobufUtil.deserializePayload(data)
                    }
                }

                else -> throw Exception("Unknown format")
            }
        }

        return ImportedData(accounts)
    }

    /**
     * Parses the given [uri] into an [Account].
     *
     * Note: This won't deserialize the uri if it was serialized using Protocol Buffers.
     */
    fun parseAccount(uri: String): Account {
        return parseAccount(Uri.parse(uri))
    }

    /**
     * Parses the given [uri] into an [Account].
     *
     * Note: This won't deserialize the uri if it was serialized using Protocol Buffers.
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
     *
     * Note: This won't serialize the data using Protocol Buffers, use the export methods instead.
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

    /**
     * Container for the imported data
     */
    data class ImportedData(
        val accounts: List<Account> = listOf(),
//        val tags: List<Tag> = emptyList(),
    ) : Serializable

    enum class UriType {
        Unknown,
        Plain,
        Backup,
        GoogleAuthenticatorBackup
    }
}