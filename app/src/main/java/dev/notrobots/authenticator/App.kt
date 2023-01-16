package dev.notrobots.authenticator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobScheduler
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
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
                                //FIXME: If you don't specify the parameter name there are two conflicting constructors
        val jobLogger = Logger(tag = "JobScheduler")
        val jobScheduler = getSystemService<JobScheduler>()
        val jobs = jobScheduler?.allPendingJobs

        if (jobs.isNullOrEmpty()) {
            jobLogger.logi("No scheduled jobs")
        }

        jobs?.forEachIndexed { i, job ->
            jobLogger.logi("--- Job $i ---")
            jobLogger.logi("Owner: ${job.service.packageName}")
            jobLogger.logi("Service: ${job.service.className}")
            jobLogger.logi("IsPeriodic: ${job.isPeriodic}")
            jobLogger.logi("IsPersisted: ${job.isPersisted}")
            jobLogger.logi("IntervalMillis: ${job.intervalMillis}")
        }
    }
}