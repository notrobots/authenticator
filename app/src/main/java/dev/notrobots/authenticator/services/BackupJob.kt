package dev.notrobots.authenticator.services

import android.app.job.JobService
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.db.AccountDao
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

    companion object {
        const val JOB_ID = -1
    }
}