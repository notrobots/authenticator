package dev.notrobots.authenticator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
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

        LogUtil.setTag(LOG_TAG)
    }

    companion object {
        const val LOG_TAG = "OTP"
        const val NOTIFICATION_CHANNEL_BACKUPS = "NOTIFICATION_CHANNELS.Backups"
        //TODO This should also change based on the selected totp indicator type
        const val TOTP_INDICATOR_UPDATE_DELAY = 25L //TODO Battery saver should increase this to something like 200-500

        /**
         * The max amount of bytes a QR code can hold.
         */
        //TODO: There should be a list of sizes: 64, 128, 256, 512
        const val QR_MAX_BYTES = 512       // Max: 2953

        /**
         * Size in pixels of the generated QR codes.
         */
        //TODO: Let the user chose the resolution 264, 512, 1024, 2048
        const val QR_BITMAP_SIZE = 512
    }
}