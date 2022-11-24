package dev.notrobots.authenticator.services

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.TagDao
import javax.inject.Inject

@AndroidEntryPoint
abstract class BackupJob : JobService() {
    protected val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    protected val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }
    @Inject
    protected lateinit var accountDao: AccountDao
    @Inject
    protected lateinit var tagDao: TagDao
    @Inject
    protected lateinit var accountTagCrossRefDao: AccountTagCrossRefDao

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