package dev.notrobots.authenticator.data

import dev.notrobots.preferences2.annotations.BooleanPreference
import dev.notrobots.preferences2.annotations.EnumPreference
import dev.notrobots.preferences2.annotations.LongPreference
import dev.notrobots.preferences2.annotations.StringPreference

object Preferences {
    @BooleanPreference(true)
    const val COLLAPSE_PINS = "collapse_pins"
    @BooleanPreference(true)
    const val COLLAPSE_ICONS = "collapse_icons"
    @BooleanPreference(false)
    const val HIDE_PINS = "hide_pins"
    @BooleanPreference
    const val ALLOW_SCREENSHOTS = "allow_screenshots"
    @BooleanPreference
    const val HIDE_PINS_AUTOMATICALLY = "hide_pins_automatically"
    @StringPreference   // Stored as String, used as Long
    const val HIDE_PINS_DELAY = "hide_pins_delay"
    @BooleanPreference
    const val HIDE_PINS_ON_CHANGE = "hide_pins_on_change"
    @EnumPreference
    const val SORT_MODE = "sort_mode"
    @EnumPreference
    const val TOTP_INDICATOR = "totp_indicator"
    @EnumPreference
    const val APP_THEME = "app_theme"
    @BooleanPreference
    const val DYNAMIC_COLORS = "dynamic_colors"
    @BooleanPreference
    const val APP_LOCK = "app_lock"
    @BooleanPreference
    const val EXPORT_LOCK = "export_lock"
    @EnumPreference
    const val PIN_TEXT_SIZE = "pin_text_size"

    //TODO: Some values should have "enabled" at the end
    //TODO: All values that have anything to do with automatic backups should have "AUTOMATIC_BACKUP" prefix

    /**
     * The value stored are the minutes since midnight.
     *
     * E.g.
     * ```
     * 121 minutes  =   02 : 01
     * 500 minutes  =   08 : 20
     * 1439 minutes =   23 : 59
     * ```
     */
//    @LongPreference
//    const val AUTOMATIC_BACKUP_TIME = "automatic_backup_time"

    @BooleanPreference
    const val LOCAL_BACKUP_ENABLED = "local_backup_enabled"

    @StringPreference   //TODO: Add an option to preferences2 that can read a string (or any value) and convert it to the required type
    const val LOCAL_BACKUP_FREQUENCY = "local_backup_frequency"

    @StringPreference
    const val LOCAL_BACKUP_PATH = "local_backup_path"

    @LongPreference
    const val LAST_LOCAL_BACKUP_TIME = "last_local_backup_time"

    @StringPreference
    const val LAST_LOCAL_BACKUP_PATH = "last_local_backup_path"

    @BooleanPreference
    const val DRIVE_BACKUP_ENABLED = "drive_backup_enabled"

    @StringPreference
    const val DRIVE_BACKUP_PATH = "drive_backup_path"

    @StringPreference
    const val DRIVE_BACKUP_FREQUENCY = "drive_backup_frequency"
}