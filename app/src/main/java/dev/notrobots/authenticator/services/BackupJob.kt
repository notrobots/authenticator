package dev.notrobots.authenticator.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.models.JsonSerializable
import javax.inject.Inject

@AndroidEntryPoint
abstract class BackupJob : JobService() {
    protected val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    protected val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }
    protected val logger = Logger(this::class.simpleName)

    @Inject
    protected lateinit var accountDao: AccountDao

    @Inject
    protected lateinit var tagDao: TagDao

    @Inject
    protected lateinit var accountTagCrossRefDao: AccountTagCrossRefDao

    protected fun sendNotification(notification: Notification, id: Int) {
        @SuppressLint("MissingPermission")
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(id, notification)
        } else {
            Logger.logw("Cannot send notification from. Permission not granted.")
        }
    }

    companion object {
        const val JOB_ID = -1

        inline fun <reified T> schedule(
            context: Context,
            id: Int,
            interval: Long,
            jobInfoFactory: JobInfo.Builder.() -> Unit = {}
        ): Int {
            val componentName = ComponentName(context, T::class.java)
            val jobInfo = JobInfo.Builder(id, componentName)
                .setPeriodic(interval)
                .setPersisted(true)
                .apply(jobInfoFactory)
                .build()
            val jobScheduler = context.getSystemService<JobScheduler>()!!

            return jobScheduler.schedule(jobInfo)
        }
    }
}