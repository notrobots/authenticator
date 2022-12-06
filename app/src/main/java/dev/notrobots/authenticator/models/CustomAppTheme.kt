package dev.notrobots.authenticator.models

import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import dev.notrobots.authenticator.R

enum class CustomAppTheme(
    @StyleRes val id: Int,
    @ColorRes val primaryColor: Int
) {
    Amber(R.style.ThemeOverlay_Amber, R.color.amber_primary),
    Blue(R.style.ThemeOverlay_Blue, R.color.blue_primary),
    BlueVariant(R.style.ThemeOverlay_BlueVariant, R.color.blue_variant_primary),
    Brown(R.style.ThemeOverlay_Brown, R.color.brown_primary),
    Cyan(R.style.ThemeOverlay_Cyan, R.color.cyan_primary),
    DeepOrange(R.style.ThemeOverlay_DeepOrange, R.color.deep_orange_primary),
    DeepPurple(R.style.ThemeOverlay_DeepPurple, R.color.deep_purple_primary),
    Green(R.style.ThemeOverlay_Green, R.color.green_primary),
    Indigo(R.style.ThemeOverlay_Indigo, R.color.indigo_primary),
    LightBlue(R.style.ThemeOverlay_LightBlue, R.color.light_blue_primary),
    LightGreen(R.style.ThemeOverlay_LightGreen, R.color.light_green_primary),
    Lime(R.style.ThemeOverlay_Lime, R.color.lime_primary),
    Orange(R.style.ThemeOverlay_Orange, R.color.orange_primary),
    Pink(R.style.ThemeOverlay_Pink, R.color.pink_primary),
    Purple(R.style.ThemeOverlay_Purple, R.color.purple_primary),
    Red(R.style.ThemeOverlay_Red, R.color.red_primary),
    Teal(R.style.ThemeOverlay_Teal, R.color.teal_primary),
    Violet(R.style.ThemeOverlay_Violet, R.color.violet_primary),
    Yellow(R.style.ThemeOverlay_Yellow, R.color.yellow_primary)
}