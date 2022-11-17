package dev.notrobots.authenticator.util

import android.net.Uri
import androidx.core.net.toUri
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.extensions.contains
import dev.notrobots.authenticator.extensions.getTags
import dev.notrobots.authenticator.extensions.replaceQueryParameter
import dev.notrobots.authenticator.models.*
import org.apache.commons.codec.binary.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Replacement for the actual [AccountWithTags] class.
 *
 * When loading data from a backup there is no way to associate an account with its tags using
 * the [AccountWithTags] class, so we're using a Map that associates an Account with a List of Tags.
 */
private typealias AccountsWithTags = Map<Account, List<String>>

object BackupManager {
    private val authenticatorBackupSerializer = AuthenticatorBackupSerializer()
    private val googleAuthenticatorBackupSerializer = GoogleAuthenticatorBackupSerializer()

    const val BACKUP_URI_SCHEME = "otpauth"
    const val BACKUP_URI_AUTHORITY = "backup"
    const val BACKUP_URI_DATA = "data"
    const val BACKUP_URI_PART = "part"
    const val BACKUP_URI_TOTAL = "total"
    const val BACKUP_GOOGLE_URI_SCHEME = "otpauth-migration"
    const val BACKUP_GOOGLE_URI_AUTHORITY = "offline"
    const val BACKUP_GOOGLE_URI_DATA = "data"
    const val BACKUP_JSON_ACCOUNTS = "accounts"
    const val BACKUP_JSON_TAGS = "tags"
    const val BACKUP_JSON_SETTINGS = "settings"

    val textBackupFilename
        get() = "authenticator_${now() / 100}.txt"
    val jsonBackupFilename
        get() = "authenticator_${now() / 100}.json"
    val qrBackupFilename
        get() = "authenticator_qr_${now() / 100}.png"
    val googleAuthenticatorBackupFilename
        get() = "google_authenticator_${now() / 100}.png"

    /**
     * Exports the items as a single line string.
     *
     * The items are serialized using the protocol buffer message
     * defined in `app/src/main/proto/Authenticator.proto`.
     *
     * ```
     * E.g.
     * lN8QQ%9bbd50SeE1ZdEoe%dKfsZrmJgEzNYVG7UKbxlvrd25Fl7D
     * ```
     *
     * Exported items:
     * + Accounts
     * + Tags
     */
    fun exportString(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>
    ): String {
        return authenticatorBackupSerializer.serialize(
            accounts,
            accountsWithTags,
            tags
        ).elementAt(0)
    }

    /**
     * Exports the items as a list of [Uri]s.
     *
     * This is meant for internal use.
     *
     * ```
     * E.g.
     * otpauth://totp/label1:name1?secret={SECRET}
     * otpauth://totp/label2:name2?secret={SECRET}&tags=tag3,tag4
     * otpauth://hotp/label3:name3?secret={SECRET}&tags=tag1,tag2
     * otpauth://tag/name1
     * otpauth://tag/name2
     * ```
     *
     * Exported items:
     * + Accounts
     * + Tags
     */
    fun exportUris(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>
    ): List<Uri> {
        return tags.map(Tag::toUri) + accounts.map {
            val accountWithTags = accountsWithTags.find { a -> a.account == it }

            if (accountWithTags?.tags != null) {
                Account.toUri(it, accountWithTags.tags)
            } else {
                Account.toUri(it)
            }
        }
    }

    /**
     * Exports the items as one or more [QRCode].
     *
     * The items are serialized using the protocol buffer message
     * defined in `app/src/main/proto/Authenticator.proto` and then the
     * resulting string is split based on [App.QR_MAX_BYTES].
     *
     * The resulting [QRCode]s must all be imported or the imported data won't be readable.
     *
     * ```
     * E.g.
     * QR1: otpauth://backup?part=1&total=2&data={SERIALIZED_DATA_CHUNK_1}
     * QR2: otpauth://backup?part=2&total=2&data={SERIALIZED_DATA_CHUNK_2}
     * ```
     *
     * Exported items:
     * + Accounts
     * + Tags
     */
    fun exportQR(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>
    ): List<QRCode> {
        val uri = Uri.Builder()
            .scheme(BACKUP_URI_SCHEME)
            .authority(BACKUP_URI_AUTHORITY)
            .appendQueryParameter(BACKUP_URI_PART, "999")
            .appendQueryParameter(BACKUP_URI_TOTAL, "999")
            .appendQueryParameter(BACKUP_URI_DATA, "")

        val data = authenticatorBackupSerializer.serialize(
            accounts,
            accountsWithTags,
            tags,
            App.QR_MAX_BYTES - uri.build().toString().length
        )

        return data.mapIndexed { index, s ->
            ///xxx Will this append it or replace it?
            uri.replaceQueryParameter(BACKUP_URI_PART, (index + 1).toString())
            uri.replaceQueryParameter(BACKUP_URI_TOTAL, data.size.toString())
            uri.replaceQueryParameter(BACKUP_URI_DATA, s)

            QRCode(uri.build(), App.QR_BITMAP_SIZE)
        }
    }

    /**
     * Exports the items as a string containing a list of [Uri]s.
     *
     * ```
     * E.g.
     * otpauth://totp/label1:name1?secret={SECRET}
     * otpauth://totp/label2:name2?secret={SECRET}&tags=tag3,tag4
     * otpauth://hotp/label3:name3?secret={SECRET}&tags=tag1,tag2
     * otpauth://tag/name1
     * otpauth://tag/name2
     * ```
     *
     * Exported items:
     * + Accounts
     * + Tags
     */
    fun exportPlainText(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>
    ): String {
        return exportUris(
            accounts,
            accountsWithTags,
            tags
        ).joinToString("\n")
    }

    /**
     * Exports the items as a list of [QRCode].
     *
     * ```
     * E.g.
     * QR1: otpauth://totp/label1:name1?secret={SECRET}
     * QR2: otpauth://totp/label2:name2?secret={SECRET}
     * QR3: otpauth://hotp/label3:name3?secret={SECRET}
     * ```
     *
     * Exported items:
     * + Accounts
     */
    //TODO: This option should be deprecated and instead the user should be able to quickly export an account on the AccountListActivity
    @Deprecated("Do not export each account as a single QR code")
    fun exportPlainQR(
        accounts: List<Account>
    ): List<QRCode> {
        return exportUris(accounts, emptyList(), emptyList()).map {
            QRCode(it, App.QR_BITMAP_SIZE)
        }
    }

    /**
     * Exports the items as a JSON object.
     *
     * ```
     * E.g.
     * {
     *     "accounts": [],
     *     "tags": [],
     *     "settings": {}
     * }
     * ```
     *
     * Exported items:
     * + Accounts
     * + Tags
     * + Settings
     */
    fun exportJson(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>,
        settings: Map<String, Any?>
    ): JSONObject {
        return JSONObject(
            mapOf(
                BACKUP_JSON_ACCOUNTS to JSONArray(
                    accounts.map {
                        Account.toJson(it, accountsWithTags.getTags(it)!!)  //XXX Technically this is always not null
                    }
                ),
                BACKUP_JSON_TAGS to JSONArray(
                    tags.map(Tag::name) //TODO: Most .map could be reduced to this
                )
            )
        )
    }

    /**
     * Exports the items as one or more [QRCode], the content is serialized
     * using Google Authenticator's protobuf definition and split based on [App.QR_MAX_BYTES].
     *
     * The items are serialized using using the protocol buffer message defined
     * in `app/src/main/proto/GoogleAuthenticator.proto` and then each group is split based on [App.QR_MAX_BYTES].
     *
     * ```
     * E.g.
     * QR1: otpauth-migration://offline?data={SERIALIZED_DATA_CHUNK_1}
     * QR2: otpauth-migration://offline?data={SERIALIZED_DATA_CHUNK_2}
     * ```
     *
     * Exported items:
     * + Accounts
     */
    fun exportGoogleAuthenticator(accounts: List<Account>): List<QRCode> {
        val prefix = "otpauth-migration://offline?data="
        val data = googleAuthenticatorBackupSerializer.serialize(
            accounts,
            emptyList(),
            emptyList(),
            App.QR_MAX_BYTES - prefix.length
        )

        return data.map {
            QRCode(prefix + it, App.QR_BITMAP_SIZE)
        }
    }

//    fun exportMicrosoftAuthenticator(accounts: List<Account>): String {
//        TODO()
//    }

    /**
     * Imports the given string [data].
     *
     * Supported formats
     * + List [Uri]s separated by a new line
     *          + `otpauth://totp/label:name?secret={SECRET}&tags=tag1,tag2`
     *          + `otpauth://tag/name`
     *          + `otpauth://backup?part=1&total=1&data={SERIALIZED_DATA_CHUNK_1}`
     *          + `otpauth-migration://offline?data={SERIALIZED_DATA_CHUNK_1}`
     * + JSON string
     */
    fun importText(data: String): BackupData {
        try {
            return importJson(JSONObject(data))
        } catch (_: Exception) {
        }

        if (Base64.isBase64(data)) {
            try {
                importAuthenticatorBackup(data)
            } catch (_: Exception) {
            }
        }

        val lines = data.lines()

        return importList(lines)
    }

    /**
     * Imports the given [list].
     *
     * Supported formats
     * + List of [Uri]s
     *          + `otpauth://totp/label:name?secret={SECRET}&tags=tag1,tag2`
     *          + `otpauth://tag/name`
     *          + `otpauth://backup?part=1&total=1&data={SERIALIZED_DATA_CHUNK_1}`
     *          + `otpauth-migration://offline?data={SERIALIZED_DATA_CHUNK_1}`
     */
    fun importList(list: List<String>): BackupData {
        val tags = mutableListOf<Tag>()
        val accounts = mutableListOf<Account>()
        val accountsWithTags = mutableMapOf<Account, List<String>>()
        lateinit var authenticatorBackupData: Array<String?>
        var authenticatorBackupCount = -1

        for ((index, item) in list.withIndex()) {
            try {
                val uri = item.toUri()

                when {
                    // Google Authenticator backup
                    uri.scheme == BACKUP_GOOGLE_URI_SCHEME && uri.authority == BACKUP_GOOGLE_URI_AUTHORITY -> {
                        val data = uri.getQueryParameter(BACKUP_GOOGLE_URI_DATA)

                        require(data != null && data.isNotBlank()) {
                            "$BACKUP_GOOGLE_URI_SCHEME://$BACKUP_GOOGLE_URI_AUTHORITY?[DATA_MISSING]"
                        }

                        val (_accounts) = googleAuthenticatorBackupSerializer.deserialize(data)

                        accounts.addAll(_accounts)
                    }

                    // Authenticator backup
                    uri.scheme == BACKUP_URI_SCHEME && uri.authority == BACKUP_URI_AUTHORITY -> {
                        val total = uri.getQueryParameter(BACKUP_URI_TOTAL)?.toIntOrNull()
                        val part = uri.getQueryParameter(BACKUP_URI_PART)?.toIntOrNull()
                        val data = uri.getQueryParameter(BACKUP_URI_DATA)

                        require(total != null && total > 0) {
                            "$BACKUP_URI_SCHEME://$BACKUP_URI_AUTHORITY?total=[INVALID]"
                        }

                        require(part != null && part > 0) {
                            "$BACKUP_URI_SCHEME://$BACKUP_URI_AUTHORITY?part=[INVALID]"
                        }

                        require(part < total) {
                            "$BACKUP_URI_SCHEME://$BACKUP_URI_AUTHORITY?part=[OUT_OF_BOUNDS]"
                        }

                        require(data != null && data.isNotBlank()) {
                            "$BACKUP_URI_SCHEME://$BACKUP_URI_AUTHORITY?[DATA_MISSING]"
                        }

                        require(authenticatorBackupCount == -1 || authenticatorBackupCount == total) {
                            "$BACKUP_URI_SCHEME://$BACKUP_URI_AUTHORITY?total=[VALUE_MISMATCH]"
                        }

                        if (authenticatorBackupCount == -1) {
                            authenticatorBackupCount = total
                            authenticatorBackupData = arrayOfNulls(total)
                        }

                        require(authenticatorBackupData[part] == null) {
                            "$BACKUP_URI_SCHEME://$BACKUP_URI_AUTHORITY?part=[ALREADY_EXISTS]"
                        }

                        authenticatorBackupData[part] = data
                    }

                    // Account or Tag
                    uri.scheme == Account.URI_SCHEME -> {
                        when {
                            // Tag
                            uri.authority == Tag.URI_AUTHORITY -> {
                                tags.add(Tag.fromUri(uri))
                            }

                            // Account
                            OTPType.contains(uri.authority) -> {
                                val account = Account.fromUri(uri)

                                if (uri.contains(Account.TAGS)) {
                                    accountsWithTags[account] = Account.tagsFromUri(uri)
                                }

                                accounts.add(account)
                            }

                            else -> {
                                throw Exception("Error parsing Uri #${index + 1}: ${Account.URI_SCHEME}://[UNKNOWN]")
                            }
                        }
                    }

                    else -> throw Exception("Unknown Uri")
                }

                if (authenticatorBackupCount > 0) {
                    require(authenticatorBackupData.none { it == null }) {
                        val indexes = authenticatorBackupData.withIndex()
                            .filter { it.value == null }
                            .map { it.index }
                            .joinToString(" & ")

                        "Missing backup parts: $indexes; Total: $authenticatorBackupCount"
                    }

                    val data = authenticatorBackupData.joinToString("")
                    val (_accounts, _tags, _accountsWithTags) = importAuthenticatorBackup(data)

                    accounts.addAll(_accounts)
                    tags.addAll(_tags)
                    accountsWithTags.putAll(_accountsWithTags)
                }
            } catch (e: Exception) {
                throw Exception("Error parsing Uri #${index + 1}: ${e.message}")
            }
        }

        return BackupData(accounts, tags, accountsWithTags)
    }

    /**
     * Imports the given [json] object.
     */
    fun importJson(json: JSONObject): BackupData {
        val accounts = mutableListOf<Account>()
        val accountsWithTags = mutableMapOf<Account, List<String>>()
        val tags = mutableListOf<Tag>()
        val settings = mutableMapOf<String, Any?>()

        require(json.has(BACKUP_JSON_ACCOUNTS)) {
            "Missing '$BACKUP_JSON_ACCOUNTS' field"
        }

        require(json.has(BACKUP_JSON_TAGS)) {   //todo: optional?
            "Missing '$BACKUP_JSON_TAGS' field"
        }

        require(json.has(BACKUP_JSON_SETTINGS)) {   //todo: optional?
            "Missing '$BACKUP_JSON_SETTINGS' field"
        }

        val accountsArray = json.getJSONArray(BACKUP_JSON_ACCOUNTS)

        for (i in 0 until accountsArray.length()) {
            val accountJson = accountsArray.getJSONObject(i)
            val account = Account.fromJson(accountJson)

            accounts.add(account)
            accountsWithTags[account] = Account.tagsFromJson(accountJson)
        }

        val tagsArray = json.getJSONArray(BACKUP_JSON_TAGS)

        for (i in 0 until tagsArray.length()) {
            val tagName = tagsArray[i]

            require(tagName is String) {
                "Tag list must be only contain strings"
            }

            tags.add(Tag(tagName))
        }

        val settingsJson = json.getJSONObject(BACKUP_JSON_SETTINGS)

        for (field in settingsJson.keys()) {
            settings[field] = settingsJson[field]
        }

        return BackupData(accounts, tags, accountsWithTags, settings)
    }

    /**
     * Imports a backup that was serialized using [AuthenticatorBackupSerializer].
     */
    fun importAuthenticatorBackup(data: String): BackupData {
        require(Base64.isBase64(data)) {
            "Backup is corrupted"
        }

        val (accounts, tags, accountsWithTags) = authenticatorBackupSerializer.deserialize(data)

        return BackupData(accounts, tags, accountsWithTags)
    }

    /**
     * Container for the imported data
     */
    data class BackupData(
        val accounts: List<Account> = listOf(),
        val tags: List<Tag> = emptyList(),
        val accountsWithTags: Map<Account, List<String>> = emptyMap(),
        val settings: Map<String, Any?> = emptyMap()
    ): Serializable
}