package dev.notrobots.authenticator.models

import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import dev.notrobots.authenticator.R

enum class CustomAppTheme(
    @StyleRes val id: Int,
    @ColorRes val primaryColor: Int
) {
    BlueAzure(R.style.AppThemeOverlay_BlueAzure, R.color.BlueAzure_primary),
    BlueBlizzard(R.style.AppThemeOverlay_BlueBlizzard, R.color.BlueBlizzard_primary),
    BlueDarkSlateBlue(R.style.AppThemeOverlay_BlueDarkSlateBlue, R.color.BlueDarkSlateBlue_primary),
    CyanAqua(R.style.AppThemeOverlay_CyanAqua, R.color.CyanAqua_primary),
    CyanTurquoise(R.style.AppThemeOverlay_CyanTurquoise, R.color.CyanTurquoise_primary),
    GreenAndroid(R.style.AppThemeOverlay_GreenAndroid, R.color.GreenAndroid_primary),
    GreenElectricLime(R.style.AppThemeOverlay_GreenElectricLime, R.color.GreenElectricLime_primary),
    GreenMalachite(R.style.AppThemeOverlay_GreenMalachite, R.color.GreenMalachite_primary),
    OrangeBurnt(R.style.AppThemeOverlay_OrangeBurnt, R.color.OrangeBurnt_primary),
    OrangeCarrotOrange(R.style.AppThemeOverlay_OrangeCarrotOrange, R.color.OrangeCarrotOrange_primary),
    PinkCandy(R.style.AppThemeOverlay_PinkCandy, R.color.PinkCandy_primary),
    PinkCottonCandy(R.style.AppThemeOverlay_PinkCottonCandy, R.color.PinkCottonCandy_primary),
    PinkFuchsia(R.style.AppThemeOverlay_PinkFuchsia, R.color.PinkFuchsia_primary),
    PinkMystic(R.style.AppThemeOverlay_PinkMystic, R.color.PinkMystic_primary),
    PurpleIndigo(R.style.AppThemeOverlay_PurpleIndigo, R.color.PurpleIndigo_primary),
    PurpleLilac(R.style.AppThemeOverlay_PurpleLilac, R.color.PurpleLilac_primary),
    RedFrenchRaspberry(R.style.AppThemeOverlay_RedFrenchRaspberry, R.color.RedFrenchRaspberry_primary),
    RedVenetianRed(R.style.AppThemeOverlay_RedVenetianRed, R.color.RedVenetianRed_primary),
    YellowAmber(R.style.AppThemeOverlay_YellowAmber, R.color.YellowAmber_primary),
    YellowCitrine(R.style.AppThemeOverlay_YellowCitrine, R.color.YellowCitrine_primary),
    YellowCream(R.style.AppThemeOverlay_YellowCream, R.color.YellowCream_primary),
    YellowVanilla(R.style.AppThemeOverlay_YellowVanilla, R.color.YellowVanilla_primary)
}