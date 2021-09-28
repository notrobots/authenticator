package dev.notrobots.authenticator.util

import android.graphics.Color

fun parseColor(input: String): Int {
    val rgb = Regex("^rgb\\((\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\)$")
    val hex = Regex("^#[A-F0-9]{6}$", RegexOption.IGNORE_CASE)
    val zeroX = Regex("^0x[A-F0-9]{6}$", RegexOption.IGNORE_CASE)

    return when {
        input.matches(hex) -> Color.parseColor(input)
        input.matches(zeroX) -> Color.parseColor(input.replace("0x", "#"))
        input.matches(rgb) -> {
            val match = rgb.matchEntire(input)!!

            Color.rgb(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt()
            )
        }
        else -> Color.WHITE
    }
}

fun parseHEXColor(color: Int): String {
    return String.format("#%06X", 0xFFFFFF and color)
}