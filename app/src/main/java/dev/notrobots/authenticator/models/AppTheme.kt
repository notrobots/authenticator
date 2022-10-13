package dev.notrobots.authenticator.models

import androidx.annotation.StyleRes
import dev.notrobots.authenticator.R

enum class AppTheme(@StyleRes val id: Int) {
    FollowSystem(0),
    Light(R.style.Theme_Light),
    Dark(R.style.Theme_Dark),
    PitchBlack(R.style.Theme_PitchBlack),
    Custom(0)
}