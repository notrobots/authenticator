package dev.notrobots.authenticator.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue
import android.widget.Toast

fun Context.makeToast(content: Any?): Toast {
    return Toast.makeText(this, content.toString(), Toast.LENGTH_SHORT).also {
        it.show()
    }
}

fun Context.copyToClipboard(content: Any?) {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(null, content.toString())

    clipboardManager.setPrimaryClip(clip)
}

fun Context.resolveColorAttribute(id: Int): Int {
    return TypedValue().run {
        theme.resolveAttribute(id, this, true)
        data
    }
}