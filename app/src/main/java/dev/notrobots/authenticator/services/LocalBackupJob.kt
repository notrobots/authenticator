package dev.notrobots.authenticator.services

import android.app.job.*
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.write
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.util.TextUtil
import dev.notrobots.preferences2.getLocalBackupPath
import dev.notrobots.preferences2.putLastLocalBackupPath
import dev.notrobots.preferences2.putLastLocalBackupTime
import kotlinx.coroutines.*

class LocalBackupJob : BackupJob() {
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + supervisorJob)

    override fun onStartJob(params: JobParameters): Boolean {
        val directoryPath = preferences.getLocalBackupPath().toUri()
        val directory = DocumentFile.fromTreeUri(this, directoryPath)
        val elapsedTime = SystemClock.elapsedRealtime()
        val fileName = "authenticator_backup_$elapsedTime"
        val file = directory?.createFile("text/plain", fileName)

        if (file != null) {
            coroutineScope.launch {
                val accounts = accountDao.getAccounts()
                //TODO: There should be different methods for exports/imports and backups
                // Backups should have a date, author and such, together with an encryption option
                // Backup options should be
                // + Plain Text
                // + Plain JSON
                // + Encrypted Text
                }

                @SuppressLint("MissingPermission")
                if (notificationManager.areNotificationsEnabled()) {
                    //TODO: This notification should show when the next backup is going to be

                    val notification = NotificationCompat.Builder(this@LocalBackupJob, App.NOTIFICATION_CHANNEL_BACKUPS)
                        .setContentTitle(getString(R.string.label_local_backup_complete))                   //XXX This is a really ugly way of showing the file name
                        .setContentText(getString(R.string.label_local_backup_complete_body, "${TextUtil.formatFileUri(directoryPath)}/$fileName"))
                        .setSmallIcon(R.drawable.ic_account)
                        .build()

                    notificationManager.notify(SystemClock.elapsedRealtimeNanos().toInt(), notification)
                }

                preferences.putLastLocalBackupTime(now())
                preferences.putLastLocalBackupPath(file.uri.toString())
                jobFinished(params, false)
            }

            return true
        } else {
            @SuppressLint("MissingPermission")
            if (notificationManager.areNotificationsEnabled()) {
                val notification = NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_BACKUPS)
                    .setContentTitle(getString(R.string.label_local_backup_failed))
                    .setSubText(getString(R.string.label_local_backup_failed_body, TextUtil.formatFileUri(directoryPath), fileName))
                    .setSmallIcon(R.drawable.ic_account)
                    .build()

                notificationManager.notify(SystemClock.elapsedRealtimeNanos().toInt(), notification)
            }

            return false
        }
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        coroutineScope.cancel()
        return true
    }

    companion object {
        const val JOB_ID = 213112116001.toInt()
    }
}