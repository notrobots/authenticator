package dev.notrobots.authenticator.models

import android.net.Uri
import dev.notrobots.authenticator.util.BackupManager

enum class ExportFormat {
    /**
     * Exports all accounts as a single line string serialized using
     * this app's protobuf definition.
     *
     * ```
     * E.g.
     * otpauth://backup?data={SERIALIZED_DATA}
     * ```
     */
    @Deprecated("Use BackupFormat.QR or BackupFormat.PlainText instead")
    Text,

    /**
     * Exports all accounts as one or more [QRCode], the content is serialized
     * using this app's protobuf definition and split based on
     * [BackupManager.QR_MAX_BYTES].
     *
     * ```
     * E.g.
     * QR1: otpauth://backup?data={SERIALIZED_DATA_CHUNK_1}
     * QR2: otpauth://backup?data={SERIALIZED_DATA_CHUNK_2}
     * ```
     */
    QR,

    /**
     * Exports all accounts as a string containing the [Uri]s, used to create each account,
     * separated by a new line.
     *
     * ```
     * E.g.
     * otpauth://totp/label1:name1?secret={SECRET}
     * otpauth://totp/label2:name2?secret={SECRET}
     * otpauth://hotp/label3:name3?secret={SECRET}
     * ```
     */
    PlainText,

    /**
     * Exports all accounts as a list of [QRCode].
     *
     * ```
     * E.g.
     * QR1: otpauth://totp/label1:name1?secret={SECRET}
     * QR2: otpauth://totp/label2:name2?secret={SECRET}
     * QR3: otpauth://hotp/label3:name3?secret={SECRET}
     * ```
     */
    PlaintQR,

    /**
     * Exports all accounts and tags as a JSON object.
     *
     * ```
     * E.g.
     * {
     *     "accounts": [],
     *     "tags": []
     * }
     * ```
     */
    Json,

    /**
     * Exports all accounts as one or more [QRCode], the content is serialized
     * using Google Authenticator's protobuf definition and split based on
     * [BackupManager.QR_MAX_BYTES].
     *
     * ```
     * E.g.
     * QR1: otpauth-migration://offline?data={SERIALIZED_DATA_CHUNK_1}
     * QR2: otpauth-migration://offline?data={SERIALIZED_DATA_CHUNK_2}
     * ```
     */
    GoogleAuthenticator,

    // MicrosoftAuthenticator,
    // andOTP,
    // FreeOTP,
}