package dev.notrobots.authenticator.extensions

import android.view.animation.Animation
import kotlin.math.abs

fun Animation.reversed() = apply {
    setInterpolator {
        abs(it - 1f)
    }
}