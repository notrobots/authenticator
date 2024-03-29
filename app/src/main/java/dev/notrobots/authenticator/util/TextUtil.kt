package dev.notrobots.authenticator.util

import android.net.Uri
import dev.notrobots.authenticator.extensions.toUri

object TextUtil {
    fun formatBackupFrequency(days: Int): String {
        return when  {
            days == 1 -> "every day"
            days == 30 -> "every month"
            days == 365 -> "every year"

            days in 31..364 -> {
                "every $days day"
            }

            else -> "every $days days"
        }
    }

    fun formatFileUri(uri: Uri?): String? {
        return formatFileUri(uri?.toString())
    }

    fun formatFileUri(uri: String?): String? {
        return if (uri != null && uri.isNotBlank()) {
            val storageRgx = Regex("\\w+$")

            uri.toUri().path?.let {
                val storage = storageRgx.find(it.substringBefore(":"))?.value

                storage + ":" + it.substringAfter(":")
            }
        } else {
            ""
        }
    }
}