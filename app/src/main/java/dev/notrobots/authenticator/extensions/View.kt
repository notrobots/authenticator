package dev.notrobots.authenticator.extensions

import android.view.View

fun View.toggleSelected(): Boolean {
    isSelected = !isSelected
    return isSelected
}