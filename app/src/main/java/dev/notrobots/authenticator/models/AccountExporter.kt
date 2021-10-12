package dev.notrobots.authenticator.models

import android.graphics.Bitmap
import android.net.Uri
import com.google.protobuf.MessageLite
import dev.notrobots.androidstuff.util.error
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.logi
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.proto.Authenticator
import dev.notrobots.authenticator.proto.Authenticator.*
import dev.notrobots.authenticator.proto.GoogleAuthenticator.*
import dev.notrobots.authenticator.util.byteString
import org.apache.commons.codec.binary.Base64

class AccountExporter {
    var exportFormat: ExportFormat = ExportFormat.Default
    var exportOutput: ExportOutput = ExportOutput.Text
    var encryptionKey: String? = null
    val maxBytes
        get() = if (exportOutput == ExportOutput.QR) QR_MAX_BYTES else -1
    val fileExtension
        get() = when (exportOutput) {
            ExportOutput.Text -> "txt"
            ExportOutput.QR -> "png"
        }

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

    fun import(): List<Account> {
        TODO()
    }

    private fun encodeProtobuf(accounts: List<Account>, protobufVariant: ProtobufVariant, chunkSize: Int): List<Uri> {
        return when (protobufVariant) {
            ProtobufVariant.Default -> accounts.map {
                Payload.Account.newBuilder()
                    .setSecret(byteString(it.secret))
                    .setName(it.name)
                    .setIssuer(it.issuer)
                    .setLabel(it.label)
                    .setType(Payload.OTPType.forNumber(it.type.ordinal))
                    .build()
            }.chunked(chunkSize) {
                it.serializedSize
            }.map {
                val payloadMessage = Payload.newBuilder()
                    .addAllAccounts(it)
                    .build()
                val encoded = encodeMessage(payloadMessage)

                Uri.parse("$EXPORT_URI$encoded")
            }
            ProtobufVariant.GoogleAuthenticator -> accounts.map {
                MigrationPayload.Account.newBuilder()
                    .setAlgorithm(MigrationPayload.Algorithm.SHA1)
                    .setDigits(MigrationPayload.DigitCount.DIGIT_COUNT_SIX) //TODO: Actually parse these
                    .setIssuer(it.issuer)
                    .setName(it.name)
                    .setSecret(byteString(it.secret))
                    .setType(MigrationPayload.OtpType.forNumber(it.type.ordinal))
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
                val encoded = encodeMessage(payloadMessage)

                Uri.parse("$EXPORT_URI_GOOGLE$encoded")
            }
        }
    }

    private fun decodeProtobuf(data: List<String>): List<Account> {
        TODO()
    }

    private fun encodeMessage(message: MessageLite): String {
        val base64 = Base64().encode(message.toByteArray())
        val string = base64.toString(Charsets.UTF_8)

        return Uri.encode(string)
    }

    private fun <T : MessageLite> decodeMessage(string: String, parser: (ByteArray) -> T): T {
        val base64 = Uri.decode(string).toByteArray(Charsets.UTF_8)
        val bytes = Base64().decode(base64)

        return parser(bytes)
    }

    companion object {
        private const val QR_BITMAP_SIZE = 512
        private const val QR_MAX_BYTES = 512       // 2953
        private const val EXPORT_URI = "otpauth://offline?data="
        private const val EXPORT_URI_GOOGLE = "otpauth-migration://offline?data="
    }
}