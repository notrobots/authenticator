package dev.notrobots.authenticator.services

import android.app.job.JobParameters
import android.app.job.JobService

class DriveBackupJob : JobService() {
    override fun onStartJob(p0: JobParameters?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        return true
    }

    companion object {
        const val JOB_ID = 213112116002.toInt()
    }
}