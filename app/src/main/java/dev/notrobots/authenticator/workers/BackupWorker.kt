package dev.notrobots.authenticator.workers

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.work.*
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.data.LOCAL_BACKUP_BACKOFF_POLICY_INITIAL_DELAY
import dev.notrobots.authenticator.data.LOCAL_BACKUP_BACKOFF_POLICY_INITIAL_DELAY_UNIT
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.AuthenticatorDatabaseModule
import dev.notrobots.authenticator.db.TagDao
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

abstract class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    protected val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    protected val notificationManager by lazy {
        NotificationManagerCompat.from(context)
    }
    protected val logger = Logger(this)

    protected val accountDao: AccountDao
    protected val tagDao: TagDao
    protected val accountTagCrossRefDao: AccountTagCrossRefDao

    init {
        val db = AuthenticatorDatabaseModule.provideDatabase(applicationContext)

        accountDao = db.accountDao()
        tagDao = db.tagDao()
        accountTagCrossRefDao = db.accountTagCrossRefDao()
    }

    protected fun sendNotification(notification: Notification, id: Int) {
        @SuppressLint("MissingPermission")
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(id, notification)
        } else {
            logger.logw("Cannot send notification from. Permission not granted.")
        }
    }

    companion object {
        inline fun <reified T : ListenableWorker> schedule(
            context: Context,
            interval: Long,
            intervalUnit: TimeUnit = TimeUnit.MINUTES,
            requestBuilder: PeriodicWorkRequest.Builder.() -> Unit = {}
        ) {
            val workRequest = PeriodicWorkRequestBuilder<T>(interval, intervalUnit)
                .setInitialDelay(interval, intervalUnit)
                .apply(requestBuilder)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    LOCAL_BACKUP_BACKOFF_POLICY_INITIAL_DELAY,
                    LOCAL_BACKUP_BACKOFF_POLICY_INITIAL_DELAY_UNIT
                )
                    //TODO: Do not use the tags, save the unique ID in the preferences
                .addTag(T::class.qualifiedName ?: T::class.simpleName ?: "<NO_CLASS>")
                .build()

            WorkManager.getInstance(context)
                .enqueue(workRequest)

        }
    }
}