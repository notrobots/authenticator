package dev.notrobots.authenticator.services

import android.app.job.*
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.NOTIFICATION_CHANNEL_BACKUPS
import dev.notrobots.authenticator.extensions.isBackupJobFirstRun
import dev.notrobots.authenticator.extensions.isPersistedPermissionGranted
import dev.notrobots.authenticator.extensions.setBackupJobFirstRun
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.util.TextUtil
import dev.notrobots.preferences2.getLocalBackupPath
import kotlinx.coroutines.*

class LocalBackupJob : BackupJob() {
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + supervisorJob)

    override fun onStartJob(params: JobParameters): Boolean {
        // If this is the first time the job is run we simply skip it
        // since a job is run as soon as you schedule it
        if (preferences.isBackupJobFirstRun(JOB_ID)) {
            preferences.setBackupJobFirstRun(JOB_ID, false)
            logger.logi("First run skipped")
            return false
        }

        val directoryPath = preferences.getLocalBackupPath().toUri()

        // The write permission to directoryPath was revoked by the user
        if (!contentResolver.isPersistedPermissionGranted(directoryPath)) {
            val notification = NotificationCompat.Builder(this@LocalBackupJob, NOTIFICATION_CHANNEL_BACKUPS)
                .setContentTitle(getString(R.string.label_local_backup_complete))
                .setContentText(getString(R.string.error_local_backup_failed_no_permission))
                .setSmallIcon(R.drawable.ic_account)
                .build()

            logger.loge("Write permission was revoked")
            sendNotification(notification, SystemClock.elapsedRealtimeNanos().toInt())
            return false
        }

        val file = BackupManager.getLocalBackupFile(this, directoryPath)

        if (file != null) {
            coroutineScope.launch {
                val accounts = accountDao.getAccounts()
                val tags = tagDao.getTags()
                val accountsWithTags = accountTagCrossRefDao.getAccountsWithTags()

                BackupManager.performLocalBackup(this@LocalBackupJob, accounts, tags, accountsWithTags, file, preferences)

                //TODO: This notification should show when the next backup is going to be
                val notification = NotificationCompat.Builder(this@LocalBackupJob, NOTIFICATION_CHANNEL_BACKUPS)
                    .setContentTitle(getString(R.string.label_local_backup_complete))                   //XXX This is a really ugly way of showing the file name
                    .setContentText(getString(R.string.label_local_backup_complete_body, "${TextUtil.formatFileUri(directoryPath)}"))
                    .setSmallIcon(R.drawable.ic_account)
                    .build()

                sendNotification(notification, SystemClock.elapsedRealtimeNanos().toInt())
                jobFinished(params, false)
            }

            return true
        } else {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_BACKUPS)
                .setContentTitle(getString(R.string.label_local_backup_failed))
                //TODO: You should check if this is actually displayed when the user removes the storage permission and a backup job is about to happen
                // To test this: setup a backupjob and shortly after remove either the storage permission or the access to this directory specifically
                // If that does break it, it means you should check whenever a file needs to be written if the permission was granted
                .setSubText(getString(R.string.label_local_backup_failed_body, TextUtil.formatFileUri(directoryPath)))
                .setSmallIcon(R.drawable.ic_account)
                .build()

            logger.loge("DocumentFile was null")
            sendNotification(notification, SystemClock.elapsedRealtimeNanos().toInt())

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