package dev.notrobots.authenticator.extensions

import android.content.res.TypedArray
import androidx.annotation.StyleableRes
import androidx.core.content.res.getResourceIdOrThrow

internal fun TypedArray.getResourceIdOrNull(@StyleableRes index: Int): Int? {
    return try {
        getResourceIdOrThrow(index)
    } catch (e: Exception) {
        null
    }
}