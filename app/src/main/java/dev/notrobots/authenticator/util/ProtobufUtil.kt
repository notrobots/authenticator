package dev.notrobots.authenticator.util

import android.net.Uri
import com.google.protobuf.ByteString
import com.google.protobuf.Internal
import com.google.protobuf.MessageLite
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.proto.Authenticator.*
import dev.notrobots.authenticator.proto.GoogleAuthenticator.*
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64

object ProtobufUtil {
    //TODO: There should be a base class that serializes/deserializes data using protobuf and
    // each implementation should have its own class
    fun serializePayload(accounts: List<Account>, variant: Variant, maxBytes: Int = 0): List<Uri> {
        val chunk = mutableSetOf<MessageLite>()
        var chunkSize = 0
        val uris = mutableListOf<Uri>()
        val serializeChunk = {
            val payload = MigrationPayload.newBuilder()

            for (message in chunk) {
                when (message) {
                    is MigrationPayload.Account -> payload.addAccounts(message)
                }
            }

            uris += Uri.Builder()
                .scheme(AccountExporter.BACKUP_OTP_SCHEME)
                .authority(AccountExporter.BACKUP_OTP_AUTHORITY)
                .appendQueryParameter(AccountExporter.BACKUP_OTP_DATA, encodeMessage(payload.build()))
                .build()
        }

        if (maxBytes <= 0) {
            for (account in accounts) {
                chunk.add(serializeAccount(account, variant))
            }
            serializeChunk()
        } else {
            //TODO: Try and eventually fix this
            for (cursor in accounts.indices) {
                val account = serializeAccount(accounts[cursor], variant)

                if (getMessageLength(account) + chunkSize > maxBytes) {
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

    fun deserializePayload(payload: String): List<Account> {
        val payload = MigrationPayload.parseFrom(decodeMessage(payload))

        return List(payload.accountsCount) {
            deserializeAccount(payload.accountsList[it])
        }
    }

    fun serializeAccount(account: Account, variant: Variant): MessageLite {
        return when (variant) {
            Variant.Default -> MigrationPayload.Account.newBuilder()
                .setName(account.name)
                .setOrder(account.order)
                .setSecret(encodeSecret(account.secret))
                .setIssuer(account.issuer)
                .setLabel(account.label)
                //TODO: Maybe make an extension for the MigrationPayload.Account class?
                .setType(serializeOTPType(account.type, variant))
                .setCounter(account.counter)
                .setDigits(account.digits)
                .setPeriod(account.period)
                .setAlgorithm(serializeAlgorithm(account.algorithm, variant))
                .build()
            Variant.GoogleAuthenticator -> TODO()
        }
    }

    fun deserializeAccount(account: MigrationPayload.Account): Account {
        return Account(
            account.name,
            decodeSecret(account.secret)
        ).apply {
            order = account.order
            issuer = account.issuer
            label = account.label
            type = deserializeOTPType(account.type)
            counter = account.counter
            digits = account.digits
            period = account.period
            algorithm = deserializeAlgorithm(account.algorithm)
        }
    }

    fun getMessageLength(messageLite: MessageLite): Int {
        return 4 * (messageLite.serializedSize / 3)
    }

    private fun <E : Internal.EnumLite> serializeAlgorithm(algorithm: HmacAlgorithm, variant: Variant): E {
        return when (variant) {
            Variant.Default -> when (algorithm) {
                HmacAlgorithm.SHA1 -> MigrationPayload.Algorithm.ALGORITHM_SHA1
                HmacAlgorithm.SHA256 -> MigrationPayload.Algorithm.ALGORITHM_SHA256
                HmacAlgorithm.SHA512 -> MigrationPayload.Algorithm.ALGORITHM_SHA512

                else -> MigrationPayload.Algorithm.ALGORITHM_SHA1
            }

            Variant.GoogleAuthenticator -> when (algorithm) {
                HmacAlgorithm.SHA1 -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA1
                HmacAlgorithm.SHA256 -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA256
                HmacAlgorithm.SHA512 -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA512

                else -> GoogleMigrationPayload.Algorithm.ALGORITHM_SHA1
            }
        } as E
    }

    private fun deserializeAlgorithm(algorithm: MigrationPayload.Algorithm): HmacAlgorithm {
        return when (algorithm) {
            MigrationPayload.Algorithm.ALGORITHM_SHA1 -> HmacAlgorithm.SHA1
            MigrationPayload.Algorithm.ALGORITHM_SHA256 -> HmacAlgorithm.SHA256
            MigrationPayload.Algorithm.ALGORITHM_SHA512 -> HmacAlgorithm.SHA512

            else -> HmacAlgorithm.SHA1
        }
    }

    private fun deserializeAlgorithm(algorithm: GoogleMigrationPayload.Algorithm): HmacAlgorithm {
        return when (algorithm) {
            GoogleMigrationPayload.Algorithm.ALGORITHM_SHA1 -> HmacAlgorithm.SHA1
            GoogleMigrationPayload.Algorithm.ALGORITHM_SHA256 -> HmacAlgorithm.SHA256
            GoogleMigrationPayload.Algorithm.ALGORITHM_SHA512 -> HmacAlgorithm.SHA512

            else -> HmacAlgorithm.SHA1
        }
    }

    private fun <E : Internal.EnumLite> serializeOTPType(otpType: OTPType, variant: Variant): E {
        return when (variant) {
            Variant.Default -> when (otpType) {
                OTPType.TOTP -> MigrationPayload.OTPType.OTP_TYPE_TOTP
                OTPType.HOTP -> MigrationPayload.OTPType.OTP_TYPE_HOTP
            }
            Variant.GoogleAuthenticator -> when (otpType) {
                OTPType.TOTP -> GoogleMigrationPayload.OTPType.OTP_TYPE_TOTP
                OTPType.HOTP -> GoogleMigrationPayload.OTPType.OTP_TYPE_HOTP
            }
        } as E
    }

    private fun deserializeOTPType(type: MigrationPayload.OTPType): OTPType {
        return when (type) {
            MigrationPayload.OTPType.OTP_TYPE_TOTP -> OTPType.TOTP
            MigrationPayload.OTPType.OTP_TYPE_HOTP -> OTPType.HOTP

            else -> OTPType.TOTP
        }
    }

    private fun deserializeOTPType(type: GoogleMigrationPayload.OTPType): OTPType {
        return when (type) {
            GoogleMigrationPayload.OTPType.OTP_TYPE_TOTP -> OTPType.TOTP
            GoogleMigrationPayload.OTPType.OTP_TYPE_HOTP -> OTPType.HOTP

            else -> OTPType.TOTP
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

    private fun encodeMessage(message: MessageLite): String {
        val base64 = Base64().encode(message.toByteArray())
        val string = base64.toString(Charsets.UTF_8)

        return Uri.encode(string)
    }

    private fun decodeMessage(string: String): ByteArray {
        val base64 = Uri.decode(string).toByteArray(Charsets.UTF_8)

        return Base64().decode(base64)
    }

    /**
     * Protocol Buffer variant/implementation.
     */
    enum class Variant {
        Default,
        GoogleAuthenticator
    }
}