package dev.notrobots.authenticator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.data.LOG_DEFAULT_TAG
import dev.notrobots.authenticator.data.NOTIFICATION_CHANNEL_BACKUPS
import dev.notrobots.authenticator.util.createNotificationChannel

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_BACKUPS,
            "Backups",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        Logger.tag = LOG_DEFAULT_TAG
    }
}