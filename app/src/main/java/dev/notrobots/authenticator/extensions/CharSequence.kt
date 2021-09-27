package dev.notrobots.authenticator.extensions

import android.net.Uri

fun CharSequence.toUri(): Uri {
    return Uri.parse(toString())
}

fun CharSequence.isOnlySpaces(): Boolean {
    return trim().isEmpty() && !isEmpty()
}