package dev.notrobots.authenticator.data

import dev.notrobots.preferences2.annotations.BooleanPreference
import dev.notrobots.preferences2.annotations.EnumPreference
import dev.notrobots.preferences2.annotations.StringPreference

object Preferences {
    @BooleanPreference(true)
    const val SHOW_PINS = "show_pins"
    @BooleanPreference(true)
    const val SHOW_ICONS = "show_icons"
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
    const val APP_LOCK = "app_lock"
    @BooleanPreference
    const val EXPORT_LOCK = "export_lock"
}