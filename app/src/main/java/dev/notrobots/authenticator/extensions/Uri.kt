package dev.notrobots.authenticator.extensions

import android.net.Uri

operator fun Uri.get(name: String): String? {
    return getQueryParameter(name)
}

operator fun Uri.contains(name: String): Boolean {
    return name in queryParameterNames
}

fun Uri.Builder.replaceQueryParameter(key: String, newValue: String) = apply{
    val oldUri = build()

    clearQuery()

    for (param in oldUri.queryParameterNames) {
        appendQueryParameter(param, if (param == key) newValue else oldUri.getQueryParameter(param))
    }
}