package dev.notrobots.authenticator.models

import android.net.Uri
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.util.BackupManager

@Deprecated("Use the methods defined in the BackupManager utility")
enum class BackupFormat {
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
    String,

    /**
     * Exports the items as one or more [QRCode].
     *
     * The items are serialized using using [BackupFormat.String] and then the
     * resulting string is split based on [App.QR_MAX_BYTES].
     *
     * The resulting [QRCode]s must all be imported or the imported data won't be readable.
     *
     * ```
     * E.g.
     * Accounts
     * QR1: otpauth://backup?part=1&total=2&data={SERIALIZED_DATA_CHUNK_1}
     * QR2: otpauth://backup?part=2&total=2&data={SERIALIZED_DATA_CHUNK_2}
     * ```
     *
     * Exported items:
     * + Accounts
     * + Tags
     */
    QR,

    /**
     * Exports the items as a list of [Uri]s.
     *
     * ```
     * E.g.
     * otpauth://totp/label1:name1?secret={SECRET}
     * otpauth://totp/label2:name2?secret={SECRET}
     * otpauth://hotp/label3:name3?secret={SECRET}
     * otpauth://tag/name1
     * otpauth://tag/name2
     * ```
     *
     * Exported items:
     * + Accounts
     * + Tags
     */
    PlainText,

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
    PlainQR,

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
    Json,

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
    GoogleAuthenticator,

    // MicrosoftAuthenticator,
    // andOTP,
    // FreeOTP,
}