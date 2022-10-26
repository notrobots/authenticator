package dev.notrobots.authenticator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.HiltAndroidApp
import dev.notrobots.androidstuff.util.LogUtil

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

        LogUtil.setTag(TAG)
    }

    companion object {
        const val TAG = "OTP"
        const val NOTIFICATION_CHANNEL_BACKUPS = "NOTIFICATION_CHANNELS.Backups"
    }
}