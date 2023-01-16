@file:JvmName("NotificationUtil")

package dev.notrobots.authenticator.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi

@Retention(AnnotationRetention.SOURCE)
@IntDef(
    NotificationManager.IMPORTANCE_DEFAULT,
    NotificationManager.IMPORTANCE_HIGH,
    NotificationManager.IMPORTANCE_LOW,
    NotificationManager.IMPORTANCE_MAX,
    NotificationManager.IMPORTANCE_MIN,
    NotificationManager.IMPORTANCE_NONE,
    NotificationManager.IMPORTANCE_UNSPECIFIED
)
annotation class NotificationImportance

fun createNotificationChannel(
    context: Context,
    id: String,
    name: String,
    @NotificationImportance importance: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val backupsNotificationChannel = NotificationChannel(
            id,
            name,
            importance
        )

        notificationManager.createNotificationChannel(backupsNotificationChannel)
    }
}
