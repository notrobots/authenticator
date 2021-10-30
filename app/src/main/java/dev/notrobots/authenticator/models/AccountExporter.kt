package dev.notrobots.authenticator.models

import android.net.Uri
import com.google.protobuf.ByteString
import dev.notrobots.androidstuff.util.error
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.logi
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.proto.Authenticator.*
import dev.notrobots.authenticator.proto.GoogleAuthenticator.*
import dev.notrobots.authenticator.util.isValidBase32
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64

class AccountExporter {
    var exportFormat: ExportFormat = ExportFormat.Default
    var exportOutput: ExportOutput = ExportOutput.Text
    val maxBytes
        get() = if (exportOutput == ExportOutput.QR) QR_MAX_BYTES else -1

    /**
     * Exports the given account list based on this exporter's configuration
     *
     * The return type can be one of the following:
     * + [String]
     * + List of [QRCode]
     */
    fun export(accounts: List<Account>): Any {
        val content = when (exportFormat) {
            ExportFormat.Default -> accounts.map { it.getUri() }
            ExportFormat.Protobuf -> encodeProtobuf(accounts, ProtobufVariant.Default, maxBytes)
            ExportFormat.GoogleProtobuf -> encodeProtobuf(accounts, ProtobufVariant.GoogleAuthenticator, maxBytes)
            ExportFormat.Encrypted -> accounts.map { it.getUri() }
        }

        return when (exportOutput) {
            ExportOutput.Text -> content.joinToString("\n")
            ExportOutput.QR -> content.map { QRCode(it.toString(), QR_BITMAP_SIZE) }
        }
    }

    fun export(account: Account): Any {
        return export(account).let {
            if (it is List<*>) {
                it.first() ?: error("List is empty")
            } else {
                it
            }
        }
    }

    fun import(text: String): List<Account> {
        val uris = text.split("\n").map {
            Uri.parse(it)
        }

        return import(uris)
    }

    fun import(uris: List<Uri>): List<Account> {
        return uris.flatMap { import(it) }
    }

    fun import(uri: Uri): List<Account> {
        return when (uri.scheme) {
            EXPORT_OTP_SCHEME_GOOGLE -> decodeProtobuf(uri, ProtobufVariant.GoogleAuthenticator)
            OTP_SCHEME -> {
                if (uri.authority == EXPORT_OTP_TYPE) {
                    decodeProtobuf(uri, ProtobufVariant.Default)
                } else {
                    listOf(parseAccountUri(uri))
                }
            }

            else -> {
                error("Unknown scheme ${uri.scheme}")
            }
        }
    }

    fun importOne(text: String): Account {
        return import(text).first()
    }

    fun importOne(uri: Uri): Account {
        return import(uri).first()
    }

    private fun parseAccountUri(uri: Uri): Account {
        val typeError = { error("Type must be one of [${OTPType.values().joinToString()}]") }

        //
        // Extra optional fields
        //
        val algorithm = parseEnum(uri[OTP_ALGORITHM], true) ?: Account.DEFAULT_OTP_ALGORITHM
        val digits = uri[OTP_DIGITS]?.toIntOrNull() ?: Account.DEFAULT_OTP_DIGITS
        val counter = uri[OTP_COUNTER]?.toLongOrNull() ?: Account.DEFAULT_OTP_COUNTER
        val period = uri[OTP_PERIOD]?.toLongOrNull() ?: Account.DEFAULT_OTP_PERIOD
        val isBase32 = uri.getBooleanQueryParameter(OTP_BASE32, true)

        //
        // Required fields
        //
        val type = parseEnum<OTPType>(uri.authority?.toUpperCase()) ?: typeError()
        val path = parseAccountName(uri.path ?: error("Path malformed, must be /label:name or /name"))
        val name = path.groupValues[2]
        val secret = uri[OTP_SECRET] ?: error("Missing parameter 'secret'")

        validateName(name)
        validateSecret(secret)

        //
        // Optional fields
        //
        val label = path.groupValues[1]
        val issuer = uri[OTP_ISSUER] ?: ""

        validateLabel(label)
        validateIssuer(issuer)

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

    private fun parseAccountName(name: String): MatchResult {
        val pathError = { error("Path malformed, must be /label:name or /name") }
        val path = name.removePrefix("/")

        if (path.isBlank()) {
            pathError()
        }

        return Regex("^(?:(.+):)?(.+)$").find(path) ?: pathError()
    }

    private fun encodeProtobuf(accounts: List<Account>, protobufVariant: ProtobufVariant, chunkSize: Int): List<Uri> {
        return when (protobufVariant) {
            ProtobufVariant.Default -> accounts.map {
                Payload.Account.newBuilder()
                    .setSecret(encodeSecret(it.secret))
                    .setName(it.name)
                    .setIssuer(it.issuer)
                    .setLabel(it.label)
                    .setType(encodeOTPType(it.type))
                    .build()
            }.chunked(chunkSize) {
                it.serializedSize
            }.map {
                val payloadMessage = Payload.newBuilder()
                    .addAllAccounts(it)
                    .build()
                val encoded = encodeMessage(payloadMessage.toByteArray())

                Uri.parse("$EXPORT_URI$encoded")
            }
            ProtobufVariant.GoogleAuthenticator -> accounts.map {
                MigrationPayload.Account.newBuilder()
                    .setAlgorithm(MigrationPayload.Algorithm.SHA1)
                    .setDigits(MigrationPayload.DigitCount.DIGIT_COUNT_SIX) //TODO: Actually parse these
                    .setIssuer(it.issuer)
                    .setName(it.path)
                    .setCounter(it.counter)
                    .setSecret(encodeSecret(it.secret))
                    .setType(encodeOTPType(it.type))
                    .build()
            }.also {
                //FIXME: Chunking doesn't work
                // The chunks appear to be really small (23-24 bytes) instead of their regula size
                // This also block is just for debugging, get rid of it
                logi(it.joinToString { it.serializedSize.toString() })
            }.chunked(chunkSize) {
                it.serializedSize
            }.map {
                val payloadMessage = MigrationPayload.newBuilder()
                    .setBatchId(681385) //TODO: Random ID
                    .setBatchIndex(0)
                    .setBatchSize(1)    //FIXME: Dynamic values
                    .setVersion(1)
                    .addAllAccount(it)
                    .build()
                val encoded = encodeMessage(payloadMessage.toByteArray())

                Uri.parse("$EXPORT_URI_GOOGLE$encoded")
            }
        }
    }

    private fun decodeProtobuf(uri: Uri, protobufVariant: ProtobufVariant): List<Account> {
        val list = mutableListOf<Account>()

        when (protobufVariant) {
            ProtobufVariant.Default -> {
                val data = uri.getQueryParameter(EXPORT_OTP_DATA)

                if (data.isNullOrEmpty()) {
                    error("Data parameter is empty")
                } else {
                    val messagePayload = Payload.parseFrom(decodeMessage(data))

                    for (accountMessage in messagePayload.accountsList) {
                        list.add(
                            Account(
                                accountMessage.name,
                                decodeSecret(accountMessage.secret)
                            ).apply {
                                label = accountMessage.label
                                issuer = accountMessage.issuer
                                type = decodeOTPType(accountMessage.type)
                            }
                        )
                    }
                }
            }
            ProtobufVariant.GoogleAuthenticator -> {
                val data = uri.getQueryParameter(EXPORT_OTP_DATA_GOOGLE)

                if (data.isNullOrEmpty()) {
                    error("Data parameter is empty")
                } else {
                    val messagePayload = MigrationPayload.parseFrom(decodeMessage(data))

                    for (accountMessage in messagePayload.accountList) {
                        val path = parseAccountName(accountMessage.name)

                        list.add(
                            Account(
                                path.groupValues[2],
                                decodeSecret(accountMessage.secret)
                            ).apply {
                                label = path.groupValues[1]
                                issuer = accountMessage.issuer
                                counter = accountMessage.counter
                                type = decodeOTPType(accountMessage.type)
                            }
                        )
                    }
                }
            }
        }

        return list.toList()
    }

    private fun encodeSecret(secret: String): ByteString {
        val bytes = Base32().decode(secret)

        return ByteString.copyFrom(bytes)
    }

    private fun decodeSecret(byteString: ByteString): String {
        val bytes = Base32().encode(byteString.toByteArray())

        return bytes.toString(Charsets.UTF_8)
    }

    private fun encodeMessage(bytes: ByteArray): String {
        val base64 = Base64().encode(bytes)
        val string = base64.toString(Charsets.UTF_8)

        return Uri.encode(string)
    }

    private fun decodeMessage(string: String): ByteArray {
        val base64 = Uri.decode(string).toByteArray(Charsets.UTF_8)

        return Base64().decode(base64)
    }

    private inline fun <reified E : Enum<E>> encodeOTPType(otpType: OTPType): E {
        return when (E::class) {
            MigrationPayload.OtpType::class -> when (otpType) {
                OTPType.TOTP -> MigrationPayload.OtpType.OTP_TYPE_TOTP
                OTPType.HOTP -> MigrationPayload.OtpType.OTP_TYPE_HOTP
            }

            Payload.OTPType::class -> when (otpType) {
                OTPType.TOTP -> Payload.OTPType.TOTP
                OTPType.HOTP -> Payload.OTPType.HOTP
            }

            else -> error("Unknown OTPType class")
        } as E
    }

    private inline fun <reified E : Enum<E>> decodeOTPType(typeValue: E): OTPType {
        return when (E::class) {
            MigrationPayload.OtpType::class -> when (typeValue) {
                MigrationPayload.OtpType.OTP_TYPE_TOTP -> OTPType.TOTP
                MigrationPayload.OtpType.OTP_TYPE_HOTP -> OTPType.HOTP

                else -> OTPType.TOTP
            }

            Payload.OTPType::class -> when (typeValue) {
                Payload.OTPType.TOTP -> OTPType.TOTP
                Payload.OTPType.HOTP -> OTPType.HOTP

                else -> OTPType.TOTP
            }

            else -> error("Unknown OTPType class")
        }
    }

    companion object {
        const val QR_BITMAP_SIZE = 512
        const val QR_MAX_BYTES = 512       // 2953
        const val EXPORT_URI = "otpauth://offline?data="
        const val EXPORT_URI_GOOGLE = "otpauth-migration://offline?data="
        const val EXPORT_OTP_SCHEME = "otpauth"
        const val EXPORT_OTP_TYPE = "offline"
        const val EXPORT_OTP_DATA = "data"
        const val EXPORT_OTP_SCHEME_GOOGLE = "otpauth-migration"
        const val EXPORT_OTP_TYPE_GOOGLE = "offline"
        const val EXPORT_OTP_DATA_GOOGLE = "data"
        const val OTP_SCHEME = "otpauth"
        const val OTP_SECRET = "secret"
        const val OTP_ISSUER = "issuer"
        const val OTP_COUNTER = "counter"
        const val OTP_ALGORITHM = "algorithm"
        const val OTP_DIGITS = "digits"
        const val OTP_PERIOD = "period"
        const val OTP_BASE32 = "base32"

        /**
         * Validates the given [Account] fields and throws an exception if any of them doesn't
         * follow the requirements
         */
        fun validateSecret(secret: String) {
            if (secret.isBlank()) {
                error("Secret cannot be empty")
            }

            if (!isValidBase32(secret)) {
                error("Secret key must be a base32 string")
            }

            // This check shouldn't be need but you never know
//            if (!OTPGenerator.checkSecret(secret)) {
//                error("Invalid secret key")
//            }
        }

        fun validateIssuer(issuer: String) {
            if (issuer.isOnlySpaces()) {
                error("Issuer cannot be blank")
            }
        }

        fun validateLabel(label: String) {
            if (label.isOnlySpaces()) {
                error("Label cannot be blank")
            }
        }

        fun validateName(name: String) {
            if (name.isBlank()) {
                error("Name cannot be empty")
            }
        }
    }
}