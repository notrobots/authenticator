package dev.notrobots.authenticator.models

import android.net.Uri

interface UriSerializable<T> {
    fun toUri(value: T): Uri

    fun fromUri(uri: Uri): T

    fun fromUri(uri: String): T {
        return fromUri(Uri.parse(uri))
    }
}