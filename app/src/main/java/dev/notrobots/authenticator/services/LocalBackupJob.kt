package dev.notrobots.authenticator.services

import android.annotation.SuppressLint
import android.app.job.*
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.isBackupJobFirstRun
import dev.notrobots.authenticator.extensions.setBackupJobFirstRun
import dev.notrobots.authenticator.extensions.write
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountWithTags
import dev.notrobots.authenticator.models.Tag
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
        if (preferences.isBackupJobFirstRun(JOB_ID)) {
            // If this is the first time the job is run we simply skip it
            // since a job is run as soon as you schedule it

            preferences.setBackupJobFirstRun(JOB_ID, false)
            logd("${LocalBackupJob::class.simpleName} first run skipped")
            return false
        }

        val directoryPath = preferences.getLocalBackupPath().toUri()
        val file = BackupManager.getLocalBackupFile(this, directoryPath)

        if (file != null) {
            coroutineScope.launch {
                val accounts = accountDao.getAccounts()
                val tags = tagDao.getTags()
                val accountsWithTags = accountTagCrossRefDao.getAccountsWithTags()

                BackupManager.performLocalBackup(this@LocalBackupJob, accounts, tags, accountsWithTags, file, preferences)

                @SuppressLint("MissingPermission")
                if (notificationManager.areNotificationsEnabled()) {
                    //TODO: This notification should show when the next backup is going to be

                    val notification = NotificationCompat.Builder(this@LocalBackupJob, App.NOTIFICATION_CHANNEL_BACKUPS)
                        .setContentTitle(getString(R.string.label_local_backup_complete))                   //XXX This is a really ugly way of showing the file name
                        .setContentText(getString(R.string.label_local_backup_complete_body, "${TextUtil.formatFileUri(directoryPath)}"))
                        .setSmallIcon(R.drawable.ic_account)
                        .build()

                    notificationManager.notify(SystemClock.elapsedRealtimeNanos().toInt(), notification)
                }

                jobFinished(params, false)
            }

            return true
        } else {
            @SuppressLint("MissingPermission")
            if (notificationManager.areNotificationsEnabled()) {
                val notification = NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_BACKUPS)
                    .setContentTitle(getString(R.string.label_local_backup_failed))
                    //TODO: You should check if this is actually displayed when the user removes the storage permission and a backup job is about to happen
                    // To test this: setup a backupjob and shortly after remove either the storage permission or the access to this directory specifically
                    // If that does break it, it means you should check whenever a file needs to be written if the permission was granted
                    .setSubText(getString(R.string.label_local_backup_failed_body, TextUtil.formatFileUri(directoryPath)))
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