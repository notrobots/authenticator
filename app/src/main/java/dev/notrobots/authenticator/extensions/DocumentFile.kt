package dev.notrobots.authenticator.extensions

import android.annotation.SuppressLint
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStreamWriter

@SuppressLint("Recycle")    // Kotlin's `use` already closes the output stream
fun DocumentFile.write(
    context: Context,
    writer: OutputStreamWriter.() -> Unit
) {
    context.contentResolver.openOutputStream(uri)
        ?.writer()
        ?.use { writer(it) }
}