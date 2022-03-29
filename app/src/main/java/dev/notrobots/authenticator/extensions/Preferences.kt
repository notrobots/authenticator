package dev.notrobots.authenticator.extensions

import android.content.SharedPreferences
import androidx.core.content.edit
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.models.SortMode
import dev.notrobots.authenticator.models.TotpIndicatorType

object Preferences {
    //@Preference(type = Boolean::class, defaultValue = true)
    const val SHOW_PINS = "show_pins"
    const val SHOW_ICONS = "show_icons"
    const val SORT_MODE = "sort_mode"
    const val TOTP_INDICATOR = "totp_indicator"
}

inline fun <reified E : Enum<E>> SharedPreferences.getEnum(name: String): E? {
    val first = E::class.java.enumConstants[0]

    return getEnum(name, first)
}

inline fun <reified E : Enum<E>> SharedPreferences.getEnum(name: String, default: E?): E? {
    val value = getString(name, default.toString())

    return parseEnum<E>(value, true)
}

fun <E : Enum<E>> SharedPreferences.Editor.putEnum(name: String, value: E?) {
    putString(name, value.toString())
}

fun SharedPreferences.getShowPins(default: Boolean = false): Boolean = getBoolean(Preferences.SHOW_PINS, default)
fun SharedPreferences.setShowPins(value: Boolean) = edit { putBoolean(Preferences.SHOW_PINS, value) }
fun SharedPreferences.getShowIcons(default: Boolean = false): Boolean = getBoolean(Preferences.SHOW_ICONS, default)
fun SharedPreferences.setShowIcons(value: Boolean) = edit { putBoolean(Preferences.SHOW_ICONS, value) }
fun SharedPreferences.getSortMode(default: SortMode = SortMode.Custom): SortMode = getEnum(Preferences.SORT_MODE, default) ?: SortMode.Custom
fun SharedPreferences.setSortMode(value: SortMode) = edit { putEnum(Preferences.SORT_MODE, value) }
fun SharedPreferences.getTotpIndicatorType(default: TotpIndicatorType = TotpIndicatorType.Circular): TotpIndicatorType = getEnum(Preferences.TOTP_INDICATOR, default) ?: TotpIndicatorType.Circular
fun SharedPreferences.setTotpIndicatorType(value: TotpIndicatorType) = edit { putEnum(Preferences.TOTP_INDICATOR, value) }