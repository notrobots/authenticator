package dev.notrobots.authenticator.extensions

import android.net.Uri

internal operator fun Uri.get(name: String): String? {
    return getQueryParameter(name)
}

internal operator fun Uri.contains(name: String): Boolean {
    return name in queryParameterNames
}