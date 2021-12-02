package dev.notrobots.authenticator.extensions

import android.net.Uri

operator fun Uri.get(name: String): String? {
    return getQueryParameter(name)
}

operator fun Uri.contains(name: String): Boolean {
    return name in queryParameterNames
}