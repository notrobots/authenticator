package dev.notrobots.authenticator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.data.LOG_DEFAULT_TAG
import dev.notrobots.authenticator.data.NOTIFICATION_CHANNEL_BACKUPS

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val backupsNotificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_BACKUPS,
                "Backups",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(backupsNotificationChannel)
        }

        Logger.tag = LOG_DEFAULT_TAG
        Logger.logd("App has started")
    }
}