package dev.notrobots.authenticator.extensions

import android.content.SharedPreferences
import androidx.core.content.edit

//@Preference(type = Boolean::class, defaultValue = true)
const val SHOW_PINS = "show_pins"
const val SHOW_ICONS = "show_icons"

fun SharedPreferences.getShowPins(default: Boolean = false): Boolean = getBoolean(SHOW_PINS, default)
fun SharedPreferences.setShowPins(value: Boolean) = edit { putBoolean(SHOW_PINS, value) }
fun SharedPreferences.getShowIcons(default: Boolean = false): Boolean = getBoolean(SHOW_ICONS, default)
fun SharedPreferences.setShowIcons(value: Boolean) = edit { putBoolean(SHOW_ICONS, value) }