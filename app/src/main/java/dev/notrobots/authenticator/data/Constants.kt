package dev.notrobots.authenticator.data

import java.util.concurrent.TimeUnit

//TODO try and move constants here

const val LOG_DEFAULT_TAG = "OTP Authenticator"

const val EMOJI_RGX = "(\\u00a9|\\u00ae|[\\u2000-\\u3300]|\\ud83c[\\ud000-\\udfff]|\\ud83d[\\ud000-\\udfff]|\\ud83e[\\ud000-\\udfff])"

const val GOOGLE_AUTHENTICATOR_PROTO_VERSION = 1

// Fields max lengths
const val TAG_NAME_MAX_LENGTH = 40
const val ACCOUNT_NAME_MAX_LENGTH = 60
const val ACCOUNT_LABEL_MAX_LENGTH = 60
const val ACCOUNT_ISSUER_MAX_LENGTH = 60

// Notification channels
const val NOTIFICATION_CHANNEL_BACKUPS = "NOTIFICATION_CHANNELS.Backups"

//TODO This should also change based on the selected totp indicator type
const val TOTP_INDICATOR_UPDATE_DELAY = 25L //TODO Battery saver should increase this to something like 200-500
val HOTP_CODE_INTERVAL = TimeUnit.SECONDS.toMillis(10)

/**
 * The max amount of bytes a QR code can hold.
 */
//TODO: There should be a list of sizes: 64, 128, 256, 512
const val QR_MAX_BYTES = 512       // Max: 2953

/**
 * Size in pixels of the generated QR codes.
 */
//TODO: Let the user chose the resolution 264, 512, 1024, 2048
const val QR_BITMAP_SIZE = 512

const val SECOND_IN_MILLIS = 1000L
const val MINUTE_IN_MILLIS = 60L * SECOND_IN_MILLIS

//region Backup Worker

const val LOCAL_BACKUP_BACKOFF_POLICY_INITIAL_DELAY = 2L
val LOCAL_BACKUP_BACKOFF_POLICY_INITIAL_DELAY_UNIT = TimeUnit.MINUTES

//endregion