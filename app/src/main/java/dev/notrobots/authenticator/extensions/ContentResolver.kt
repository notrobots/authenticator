package dev.notrobots.authenticator.extensions

import android.content.ContentResolver
import android.net.Uri

fun ContentResolver.isPersistedPermissionGranted(uri: Uri): Boolean {
    val uriPermission = persistedUriPermissions.find { it.uri == uri }

    return uriPermission?.let {
        it.isWritePermission &&
        it.isReadPermission
    } ?: false
}