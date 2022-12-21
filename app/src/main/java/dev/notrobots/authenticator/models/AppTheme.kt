package dev.notrobots.authenticator.models

import androidx.annotation.StyleRes
import dev.notrobots.authenticator.R

enum class AppTheme(@StyleRes val id: Int) {
    FollowSystem(0),
    Light(R.style.AppThemeOverlay_BlueAzure),
    Dark(R.style.AppThemeOverlay_BlueAzure),
    PitchBlack(R.style.AppThemeOverlay_PitchBlack),
    Custom(0)
}