 package dev.notrobots.authenticator.models

import android.net.Uri
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.extensions.contains
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.proto.Authenticator.*
import dev.notrobots.authenticator.proto.GoogleAuthenticator.*
import dev.notrobots.authenticator.util.isValidBase32
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64
import java.io.Serializable
import java.util.*

class AccountExporter {
    fun exportText(exportData: Data): String {
        return export(exportData, 0).joinToString("\n")
    }

    fun exportQR(exportData: Data): List<QRCode> {
        return export(exportData, QR_MAX_BYTES).map { QRCode(it.toString(), QR_BITMAP_SIZE) }
    }

    fun export(exportData: Data, maxBytes: Int): List<Uri> {
        when (exportData.format) {
            BackupFormat.Plain -> {
                val groups = exportData.groups.map(::parseUri)
                val accounts = exportData.accounts.map(::parseUri)

                return accounts + groups
            }
            BackupFormat.Default -> {
                val chunk = mutableSetOf<MessageLite>()
                var chunkSize = 0
                val uris = mutableListOf<Uri>()
                val groupIds = exportData.accounts.map { it.groupId }.distinct()
                val emptyGroups = exportData.groups.filter { it.id !in groupIds }
                val serializeAccount = { it: Account ->
                    val groupName = exportData.groups.find { g -> it.groupId == g.id }?.name

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
                        .setGroup(groupName)
                        .build()
                }
                val serializeGroup = { it: AccountGroup ->
                    MigrationPayload.Group.newBuilder()
                        .setName(it.name)
                        .setOrder(it.order)
                        .build()
                }
                val serializeChunk = {
                    val payload = MigrationPayload.newBuilder()

                    for (message in chunk) {
                        when (message) {
                            is MigrationPayload.Account -> payload.addAccounts(message)
                            is MigrationPayload.Group -> payload.addGroups(message)
                        }
                    }

                    uris += Uri.Builder()
                        .scheme(EXPORT_OTP_SCHEME)
                        .authority(EXPORT_OTP_AUTHORITY)
                        .appendQueryParameter(EXPORT_OTP_DATA, encodeMessage(payload.build()))
                        .build()
                }

                if (maxBytes <= 0) {
                    for (group in exportData.groups) {
                        chunk.add(serializeGroup(group))
                    }
                    for (account in exportData.accounts) {
                        chunk.add(serializeAccount(account))
                    }
                    serializeChunk()
                } else {
                    for (cursor in exportData.accounts.indices) {
                        val groupId = exportData.accounts[cursor].groupId
                        val account = serializeAccount(exportData.accounts[cursor])
                        val group = exportData.groups.find { it.id == groupId }?.let {
                            serializeGroup(it)
                        }
                        var tmpSize = getMessageLength(account)

                        if (group != null) {
                            tmpSize += getMessageLength(group)
                        }

                        if (tmpSize + chunkSize > maxBytes) {
                            serializeChunk()
                            chunk.clear()
                            chunkSize = 0
                        }

                        if (group != null) {
                            chunk.add(group)
                            chunkSize += getMessageLength(group)
                        }

                        chunk.add(account)
                        chunkSize += getMessageLength(account)
                    }

                    for (group in emptyGroups) {
                        val g = serializeGroup(group)

                        if (chunkSize + getMessageLength(g) > maxBytes) {
                            serializeChunk()
                            chunk.clear()
                            chunkSize = 0
                        }

                        chunk.add(g)
                        chunkSize += getMessageLength(g)
                    }

                    serializeChunk()
                }

                return uris
            }
        }
    }

    fun import(text: String): Data {
        val uris = text.split("\n").map {
            Uri.parse(it)
        }

        return import(uris)
    }

    fun import(uris: List<Uri>): Data {
        val dataList = uris.map { import(it) }

        if (!dataList.all { it.format == dataList[0].format }) {
            throw Exception("Imported data can't have different formats")
        }

        return Data().apply {
            format = dataList[0].format

            for (data in dataList) {
                accounts.addAll(data.accounts)
                groups.addAll(data.groups)
            }
        }
    }

    fun import(uri: Uri): Data {
        val importData = Data()

        when (uri.scheme) {
//            EXPORT_OTP_SCHEME_GOOGLE -> decodeProtobuf(uri, ProtobufVariant.GoogleAuthenticator)

            OTP_SCHEME -> when (uri.authority) {
                EXPORT_OTP_AUTHORITY -> {
                    val data = uri.getQueryParameter(EXPORT_OTP_DATA)

                    if (data.isNullOrEmpty()) {
                        throw Exception("Data parameter is empty")
                    } else {
                        val payload = MigrationPayload.parseFrom(decodeMessage(data))

                        for (group in payload.groupsList) {
                            importData.groups.add(
                                AccountGroup(
                                    group.name
                                ).apply {
                                    order = group.order
                                }
                            )
                        }

                        for (account in payload.accountsList) {
                            val group = importData.groups.find { it.name == account.group }?.id ?: Account.DEFAULT_GROUP_ID

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
                                    groupId = group
                                }
                            )
                        }
                    }
                }

                OTP_GROUP_AUTHORITY -> importData.groups.add(parseGroup(uri))

                else -> importData.accounts.add(parseAccount(uri))
            }

            else -> {
                throw Exception("Unknown scheme ${uri.scheme}")
            }
        }

        return importData
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

    class Data : Serializable {
        var groups: MutableList<AccountGroup>
        var accounts: MutableList<Account>
        var format = BackupFormat.Plain

        val itemCount
            get() = groups.size + accounts.size

        constructor() {
            this.groups = mutableListOf()
            this.accounts = mutableListOf()
        }

        constructor(groups: List<AccountGroup>, accounts: List<Account>) {
            this.groups = groups.toMutableList()
            this.accounts = accounts.toMutableList()
        }
    }

    companion object {
        //TODO: Let the user chose the resolution 264, 512, 1024, 2048
        const val QR_BITMAP_SIZE = 512

        //TODO: There should be a list of sizes: 64, 128, 256, 512
        const val QR_MAX_BYTES = 512       // Max: 2953
        const val EXPORT_OTP_SCHEME = "otpauth"
        const val EXPORT_OTP_AUTHORITY = "offline"
        const val EXPORT_OTP_DATA = "data"
        const val EXPORT_OTP_SCHEME_GOOGLE = "otpauth-migration"
        const val EXPORT_OTP_AUTHORITY_GOOGLE = "offline"
        const val EXPORT_OTP_DATA_GOOGLE = "data"
        const val OTP_GROUP_AUTHORITY = "group"
        const val OTP_SCHEME = "otpauth"
        const val OTP_SECRET = "secret" //TODO: Rename to ACCOUNT_SECRET and GROUP_XXXX
        const val OTP_ISSUER = "issuer"
        const val OTP_COUNTER = "counter"
        const val OTP_ALGORITHM = "algorithm"
        const val OTP_DIGITS = "digits"
        const val OTP_PERIOD = "period"
        const val OTP_ORDER = "order"
        const val OTP_GROUP = "group"

        /**
         * Parses the given [uri] into a [AccountGroup]
         */
        fun parseGroup(uri: Uri): AccountGroup {
            val pathError = { throw Exception("Path malformed, must be /name") }
            val typeError = { throw Exception("Uri authority must be '${OTP_GROUP_AUTHORITY}'") }
            val type = uri.authority?.toLowerCase(Locale.getDefault()) ?: typeError()
            val name = uri.path?.removePrefix("/") ?: pathError()
            val order = uri[OTP_ORDER]?.toLongOrNull() ?: 0L //TODO: If the order is <0 or it already exists assign it the latest order

            if (type != OTP_GROUP_AUTHORITY) {
                typeError()
            }

            if (name.isBlank()) {
                pathError()
            }

            return AccountGroup(name).apply {
                this.order = order
            }
        }

        /**
         * Parses the given [group] into an [Uri]
         */
        fun parseUri(group: AccountGroup): Uri {
            return Uri.Builder()
                .scheme(OTP_SCHEME)
                .authority(OTP_GROUP_AUTHORITY)
                .path(group.name)
                .appendQueryParameter(OTP_ORDER, group.order.toString())
                .build()
        }

        /**
         * Parses the given [uri] into an [Account]
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
            val group = if (OTP_GROUP in uri) {
                uri[OTP_GROUP]?.toLongOrNull() ?: throw Exception("Group ID must be an integer")
            } else {
                Account.DEFAULT_GROUP_ID
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
                this.groupId = group
            }
        }

        /**
         * Parses the given [account] into an [Uri]
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
            uri.appendQueryParameter(OTP_GROUP, account.groupId.toString())

            return uri.build()
        }

        fun isBackup(uri: Uri): Boolean {
            val authority = uri.authority?.toLowerCase(Locale.getDefault())

            return !(uri.scheme == OTP_SCHEME && authority in OTPType.stringValues())
        }

        fun isBackup(uri: String): Boolean {
            return isBackup(Uri.parse(uri))
        }
    }
}