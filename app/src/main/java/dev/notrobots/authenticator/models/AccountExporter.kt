package dev.notrobots.authenticator.models

import android.net.Uri
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import dev.notrobots.androidstuff.util.error
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.extensions.get
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.proto.Authenticator.*
import dev.notrobots.authenticator.proto.GoogleAuthenticator.*
import dev.notrobots.authenticator.util.isValidBase32
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64

/**
 * # Account & Groups exporter
 *
 * This utility handles the exporting and importing of Accounts and Groups to and from different formats
 *
 * ### Default format
 * ```
 * Account: otpauth://(hotp|totp)/lablel:name
 *      ?secret={base32}
 *      &issuer=issuer.com
 *      &counter={int}
 *      &digits={int}
 *      &period={int}
 *      &algorithm=(sha1|sha256|sha512)
 *      &order={int}
 *      &group={int}
 * ```
 * `Group: otpauth://group/name?order={int}`
 *
 * ### Default protocol buffer
 *
 * ### Google Authenticator protocol buffer
 */
class AccountExporter {
    var exportFormat: ExportFormat = ExportFormat.Default

    @Deprecated("Use exportText or exportQR")
    var exportOutput: ExportOutput = ExportOutput.Text

    fun exportText(accounts: List<Account>, groups: List<AccountGroup>): String {
        return export(accounts, groups, exportFormat, 0).joinToString("\n")
    }

    fun exportQR(accounts: List<Account>, groups: List<AccountGroup>): List<QRCode> {
        return export(accounts, groups, exportFormat, QR_MAX_BYTES).map { QRCode(it.toString(), QR_BITMAP_SIZE) }
    }

    fun export(accounts: List<Account>, groups: List<AccountGroup>, exportFormat: ExportFormat, maxBytes: Int): List<Uri> {
        return when (exportFormat) {
            ExportFormat.Default -> {
                val accounts = accounts.map { it.getUri() }
                val groups = groups.map { it.getUri() }

                mutableListOf<Uri>().apply {
                    addAll(groups)
                    addAll(accounts)
                }
            }
            ExportFormat.Protobuf -> {
                val chunk = mutableSetOf<MessageLite>()
                var chunkSize = 0
                val uris = mutableListOf<Uri>()
                val groupIds = accounts.map { it.groupId }.distinct()
                val emptyGroups = groups.filter { it.id !in groupIds }
                val serializeAccount = { it: Account ->
                    MigrationPayload.Account.newBuilder()
                        .setSecret(encodeSecret(it.secret))
                        .setName(it.name)
                        .setIssuer(it.issuer)
                        .setLabel(it.label)
                        .setAlgorithm(encodeAlgorithm(it.algorithm))
                        .setType(encodeOTPType(it.type))
                        .build()
                        .also { s ->
                            logd("Serializing group#${it.id}: ${getMessageLength(s)}")
                        }
                }
                val serializeGroup = { it: AccountGroup ->
                    MigrationPayload.Group.newBuilder()
                        .setName(it.name)
                        .setOrder(it.order)
                        .build()
                        .also { s ->
                            logd("Serializing account#${it.id}: ${getMessageLength(s)}")
                        }
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
                        .appendQueryParameter(EXPORT_OTP_DATA, encodeMessage(payload.build().toByteArray()))
                        .build()
                }

                if (maxBytes <= 0) {
                    for (group in groups) {
                        chunk.add(serializeGroup(group))
                    }
                    for (account in accounts) {
                        chunk.add(serializeAccount(account))
                    }
                    serializeChunk()
                } else {
                    for (cursor in accounts.indices) {
                        val groupId = accounts[cursor].groupId
                        val account = serializeAccount(accounts[cursor])
                        val group = groups.find { it.id == groupId }?.let {
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

                uris
            }
        }
    }

    fun import(text: String): List<BaseAccount> {
        val uris = text.split("\n").map {
            Uri.parse(it)
        }

        return import(uris)
    }

    fun import(uris: List<Uri>): List<BaseAccount> {
        return uris.flatMap { import(it) }
    }

    fun import(uri: Uri): List<BaseAccount> {
        val items = mutableListOf<BaseAccount>()

        when (uri.scheme) {
            //
            // Google Authenticator's export scheme
            //
//            EXPORT_OTP_SCHEME_GOOGLE -> decodeProtobuf(uri, ProtobufVariant.GoogleAuthenticator)

            //
            // Standard OTP scheme
            //
            OTP_SCHEME -> when (uri.authority) {
                //
                // Protobuf Uri
                //
                EXPORT_OTP_AUTHORITY -> {
                    val data = uri.getQueryParameter(EXPORT_OTP_DATA)

                    if (data.isNullOrEmpty()) {
                        error("Data parameter is empty")
                    } else {
                        val payload = MigrationPayload.parseFrom(decodeMessage(data))

                        for (group in payload.groupsList) {
                            items.add(
                                AccountGroup(
                                    group.name
                                ).apply {
                                    order = group.order
                                }
                            )
                        }

                        for (account in payload.accountsList) {
                            items.add(
                                Account(
                                    account.name,
                                    decodeSecret(account.secret)
                                ).apply {
                                    label = account.label
                                    issuer = account.issuer
                                    type = decodeOTPType(account.type)
                                    algorithm = decodeAlgorithm(account.algorithm)
                                    period = account.period
                                    counter = account.counter
                                    digits = account.digits
                                    groupId = account.group
                                }
                            )
                        }
                    }
                }

                //
                // Group Uri
                //
                OTP_GROUP_AUTHORITY -> items.add(parseGroupUri(uri))

                //
                // Account Uri
                //
                else -> items.add(parseAccountUri(uri))
            }

            else -> {
                error("Unknown scheme ${uri.scheme}")
            }
        }

        return items
    }

//    fun importOne(text: String): Account {
//        return import(text).first()
//    }
//
//    fun importOne(uri: Uri): Account {
//        return import(uri).first()
//    }

    private fun getMessageLength(messageLite: MessageLite): Int {
        return 4 * (messageLite.serializedSize / 3)
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

    private fun parseGroupUri(uri: Uri): AccountGroup {
        val typeError = { error("Uri authority must be '${OTP_GROUP_AUTHORITY}'") }
        val type = uri.authority?.toLowerCase() ?: typeError()
        val name = uri.path ?: error("Path malformed, must be /name")
        val order = uri[OTP_ORDER]?.toLongOrNull() ?: 0L //TODO: If the order is <0 or it already exists assign it the latest order

        if (type != OTP_GROUP_AUTHORITY) {
            typeError()
        }

        if (name.isBlank()) {
            error("Path malformed, must be /name")
        }

        return AccountGroup(name).apply {
            this.order = order
        }
    }

//    private fun encodeProtobuf(accounts: List<Account>, groups: List<AccountGroup>, protobufVariant: ProtobufVariant, chunkSize: Int): List<Uri> {
//        val accounts = accounts.toMutableList()
//        val groups = groups.toMutableList()
//
//        return when (protobufVariant) {
//            ProtobufVariant.Default -> {
//
//
//                return mutableListOf<Uri>().apply {
//                    for (gwa in groupsWithAccounts) {
//                        val group = MigrationPayload.Group.newBuilder()
//                            .setName(gwa.group.name)
//                            .setOrder(gwa.group.order)
//                            .build()
//                        val accounts = gwa.accounts.map {
//                            MigrationPayload.Account.newBuilder()
//                                .setSecret(encodeSecret(it.secret))
//                                .setName(it.name)
//                                .setIssuer(it.issuer)
//                                .setLabel(it.label)
//                                .setAlgorithm(encodeAlgorithm(it.algorithm))
//                                .setType(encodeOTPType(it.type))
//                                .build()
//                        }.chunked(chunkSize - group.serializedSize) {
//                            it.serializedSize
//                        }
//
//                        accounts.map {
//                            val payload = MigrationPayload.newBuilder()
//                                .addGroups(group)
//                                .addAllAccounts(it)
//                                .build()
//                            val encoded = encodeMessage(payload.toByteArray())
//
//                            this += Uri.Builder()
//                                .scheme(EXPORT_OTP_SCHEME)
//                                .authority(EXPORT_OTP_AUTHORITY)
//                                .appendQueryParameter(EXPORT_OTP_DATA, encoded)
//                                .build()
//                        }
//                    }
//                }
//            }
////            ProtobufVariant.GoogleAuthenticator -> accounts.map {
////                GoogleMigrationPayload.Account.newBuilder()
////                    .setAlgorithm(encodeAlgorithm(it.algorithm))
////                    .setDigits(
////                        when (it.digits) {
////                            6 -> GoogleMigrationPayload.DigitCount.DIGIT_COUNT_SIX
////                            8 -> GoogleMigrationPayload.DigitCount.DIGIT_COUNT_EIGHT
////
////                            else -> GoogleMigrationPayload.DigitCount.DIGIT_COUNT_SIX
////                        }
////                    )
////                    .setIssuer(it.issuer)
////                    .setName(it.path)
////                    .setCounter(it.counter)
////                    .setSecret(encodeSecret(it.secret))
////                    .setType(encodeOTPType(it.type))
////                    .build()
////            }.chunked(chunkSize) {
////                it.serializedSize
////            }.map {
////                val payloadMessage = GoogleMigrationPayload.newBuilder()
////                    .setBatchId(681385) //TODO: Random ID
////                    .setBatchIndex(0)
////                    .setBatchSize(1)    //FIXME: Dynamic values
////                    .setVersion(1)
////                    .addAllAccounts(it)
////                    .build()
////                val encoded = encodeMessage(payloadMessage.toByteArray())
////
////                Uri.Builder()
////                    .scheme(EXPORT_OTP_SCHEME_GOOGLE)
////                    .authority(EXPORT_OTP_AUTHORITY_GOOGLE)
////                    .appendQueryParameter(EXPORT_OTP_DATA_GOOGLE, encoded)
////                    .build()
////            }
//        }
//    }

//    private fun encodeProtobuf(accounts: List<Account>, groups: List<AccountGroup>, protobufVariant: ProtobufVariant, chunkSize: Int): List<Uri> {
//        return when (protobufVariant) {
//            ProtobufVariant.Default -> {
//                val payloadAccounts = accounts.map {
//                    MigrationPayload.Account.newBuilder()
//                        .setSecret(encodeSecret(it.secret))
//                        .setName(it.name)
//                        .setIssuer(it.issuer)
//                        .setLabel(it.label)
//                        .setAlgorithm(encodeAlgorithm(it.algorithm))
//                        .setType(encodeOTPType(it.type))
//                        .build()
//                }
//                val payloadGroups = groups.map {
//                    MigrationPayload.Group.newBuilder()
//                        .setName(it.name)
//                        .setOrder(it.order)
//                        .build()
//                }
//
//                return mutableListOf<Uri>().apply {
//                    for ((i, group) in payloadGroups.withIndex()) {
//                        val groupAccounts = payloadAccounts.filter { it.group == groups[i].id }
//                        val chunk = groupAccounts.chunked(chunkSize - group.serializedSize) {
//                            it.serializedSize
//                        }
//
//                        for (accounts in chunk) {
//                            val payload = MigrationPayload.newBuilder()
//                                .addGroups(group)
//                                .addAllAccounts(accounts)
//                                .build()
//                            val encoded = encodeMessage(payload.toByteArray())
//                            val uri = Uri.Builder()
//                                .scheme(EXPORT_OTP_SCHEME)
//                                .authority(EXPORT_OTP_AUTHORITY)
//                                .appendQueryParameter(EXPORT_OTP_DATA, encoded)
//                                .build()
//
//                            add(uri)
//                        }
//                    }
//                }
//            }
//            ProtobufVariant.GoogleAuthenticator -> accounts.map {
//                GoogleMigrationPayload.Account.newBuilder()
//                    .setAlgorithm(encodeAlgorithm(it.algorithm))
//                    .setDigits(
//                        when (it.digits) {
//                            6 -> GoogleMigrationPayload.DigitCount.DIGIT_COUNT_SIX
//                            8 -> GoogleMigrationPayload.DigitCount.DIGIT_COUNT_EIGHT
//
//                            else -> GoogleMigrationPayload.DigitCount.DIGIT_COUNT_SIX
//                        }
//                    )
//                    .setIssuer(it.issuer)
//                    .setName(it.path)
//                    .setCounter(it.counter)
//                    .setSecret(encodeSecret(it.secret))
//                    .setType(encodeOTPType(it.type))
//                    .build()
//            }.chunked(chunkSize) {
//                it.serializedSize
//            }.map {
//                val payloadMessage = GoogleMigrationPayload.newBuilder()
//                    .setBatchId(681385) //TODO: Random ID
//                    .setBatchIndex(0)
//                    .setBatchSize(1)    //FIXME: Dynamic values
//                    .setVersion(1)
//                    .addAllAccounts(it)
//                    .build()
//                val encoded = encodeMessage(payloadMessage.toByteArray())
//
//                Uri.Builder()
//                    .scheme(EXPORT_OTP_SCHEME_GOOGLE)
//                    .authority(EXPORT_OTP_AUTHORITY_GOOGLE)
//                    .appendQueryParameter(EXPORT_OTP_DATA_GOOGLE, encoded)
//                    .build()
//            }
//        }
//    }

    private fun deserializeProtobuf(uri: Uri, protobufVariant: ProtobufVariant): List<Account> {
        val list = mutableListOf<Account>()

        when (protobufVariant) {
            ProtobufVariant.Default -> {
                val data = uri.getQueryParameter(EXPORT_OTP_DATA)

                if (data.isNullOrEmpty()) {
                    error("Data parameter is empty")
                } else {
                    val messagePayload = MigrationPayload.parseFrom(decodeMessage(data))

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
//            ProtobufVariant.GoogleAuthenticator -> {
//                val data = uri.getQueryParameter(EXPORT_OTP_DATA_GOOGLE)
//
//                if (data.isNullOrEmpty()) {
//                    error("Data parameter is empty")
//                } else {
//                    val messagePayload = GoogleMigrationPayload.parseFrom(decodeMessage(data))
//
//                    for (accountMessage in messagePayload.accountsList) {
//                        val path = parseAccountName(accountMessage.name)
//
//                        list.add(
//                            Account(
//                                path.groupValues[2],
//                                decodeSecret(accountMessage.secret)
//                            ).apply {
//                                label = path.groupValues[1]
//                                issuer = accountMessage.issuer
//                                counter = accountMessage.counter
//                                type = decodeOTPType(accountMessage.type)
//                            }
//                        )
//                    }
//                }
//            }
        }

        return list.toList()
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

            else -> error("Unknown OTPType class")
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

            else -> error("Unknown OTPType class")
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

            else -> error("Unknown OTPType class")
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

            else -> error("Unknown OTPType class")
        }
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
        const val OTP_SECRET = "secret"
        const val OTP_ISSUER = "issuer"
        const val OTP_COUNTER = "counter"
        const val OTP_ALGORITHM = "algorithm"
        const val OTP_DIGITS = "digits"
        const val OTP_PERIOD = "period"
        const val OTP_ORDER = "order"

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