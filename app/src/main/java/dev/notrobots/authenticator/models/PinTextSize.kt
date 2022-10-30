package dev.notrobots.authenticator.models

import androidx.annotation.StyleRes
import dev.notrobots.authenticator.R

enum class PinTextSize (@StyleRes val res: Int) {
    Small(R.style.AccountPin_Small),
    Medium(R.style.AccountPin_Medium),
    Large(R.style.AccountPin_Large)
}