package dev.notrobots.authenticator.workers

import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.net.toUri
import androidx.work.WorkerParameters
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.NOTIFICATION_CHANNEL_BACKUPS
import dev.notrobots.authenticator.extensions.isBackupJobFirstRun
import dev.notrobots.authenticator.extensions.isPersistedPermissionGranted
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.util.TextUtil
import dev.notrobots.preferences2.getLocalBackupPath
import kotlinx.coroutines.*
import java.io.File

class LocalBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : BackupWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val resources = applicationContext.resources
        val contentResolver = applicationContext.contentResolver
        val directoryPath = preferences.getLocalBackupPath().toUri()

        // The write permission to directoryPath was revoked by the user
        if (!contentResolver.isPersistedPermissionGranted(directoryPath)) {
            val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_BACKUPS)
                .setContentTitle(resources.getString(R.string.label_local_backup_complete))
                .setContentText(resources.getString(R.string.error_local_backup_failed_no_permission))
                .setSmallIcon(R.drawable.ic_account)
                .setStyle(
                    BigTextStyle().bigText(resources.getString(R.string.error_local_backup_failed_no_permission))
                )
                .build()

            logger.loge("Write permission was revoked")
            sendNotification(notification, SystemClock.elapsedRealtimeNanos().toInt())
        }

        val file = BackupManager.getLocalBackupFile(applicationContext, directoryPath)

        if (file != null) {
            val accounts = accountDao.getAccounts()
            val tags = tagDao.getTags()
            val accountsWithTags = accountTagCrossRefDao.getAccountsWithTags()

            BackupManager.performLocalBackup(applicationContext, accounts, tags, accountsWithTags, file, preferences)
            logger.logi("Backup performed")

            //TODO: This notification should show when the next backup is going to be
            val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_BACKUPS)
                .setContentTitle(resources.getString(R.string.label_local_backup_complete))                   //XXX This is a really ugly way of showing the file name
                .setContentText(resources.getString(R.string.label_local_backup_complete_body, "${TextUtil.formatFileUri(directoryPath)}"))
                .setStyle(
                    BigTextStyle().bigText(resources.getString(R.string.label_local_backup_complete_body, "${TextUtil.formatFileUri(directoryPath)}"))
                )
                .setSmallIcon(R.drawable.ic_account)
                .build()

            sendNotification(notification, SystemClock.elapsedRealtimeNanos().toInt())
            return Result.success()
        } else {
            val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_BACKUPS)
                .setContentTitle(resources.getString(R.string.label_local_backup_failed))
                //TODO: You should check if this is actually displayed when the user removes the storage permission and a backup job is about to happen
                // To test this: setup a backupjob and shortly after remove either the storage permission or the access to this directory specifically
                // If that does break it, it means you should check whenever a file needs to be written if the permission was granted
                .setSubText(resources.getString(R.string.label_local_backup_failed_body, TextUtil.formatFileUri(directoryPath)))
                .setStyle(
                    BigTextStyle().bigText(resources.getString(R.string.label_local_backup_failed_body, TextUtil.formatFileUri(directoryPath)))
                )
                .setSmallIcon(R.drawable.ic_account)
                .build()

            logger.loge("DocumentFile was null")
            sendNotification(notification, SystemClock.elapsedRealtimeNanos().toInt())

            return Result.retry()
        }
    }
}