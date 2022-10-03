package dev.notrobots.authenticator.data

import dev.notrobots.preferences2.annotations.BooleanPreference
import dev.notrobots.preferences2.annotations.EnumPreference

object Preferences {
    @BooleanPreference(true)
    const val SHOW_PINS = "show_pins"
    @BooleanPreference(true)
    const val SHOW_ICONS = "show_icons"
    @BooleanPreference(true)
    const val CLEAR_TEXT_PINS = "clear_text_pins"
    @EnumPreference
    const val SORT_MODE = "sort_mode"
    @EnumPreference
    const val TOTP_INDICATOR = "totp_indicator"
    @EnumPreference
    const val APP_THEME = "app_theme"
}