package dev.notrobots.authenticator.extensions

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.util.TypedValue
import androidx.biometric.BiometricManager
import androidx.core.content.getSystemService

/**
 * Checks if the device is secured.
 *
 * By default it checks all possible authenticators, specify [authenticators] to change this behaviour.
 */
fun Context.isDeviceSecured(authenticators: Int = -1): Boolean {
    val biometricManager = BiometricManager.from(this)

    val authenticator = biometricManager.canAuthenticate(
        if (authenticators != -1) {
            authenticators
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    )

    return authenticator == BiometricManager.BIOMETRIC_SUCCESS
}

fun Context.resolveDrawableAttribute(id: Int): Int {
    return TypedValue().run {
        theme.resolveAttribute(id, this, true)
        resourceId
    }
}

inline fun <reified T> Context.schedulePeriodicJob(
    id: Int,
    interval: Long,
    jobInfoFactory: JobInfo.Builder.() -> Unit = {}
): Int {
    val componentName = ComponentName(this, T::class.java)
    val jobInfo = JobInfo.Builder(id, componentName)
        .setPeriodic(interval)
        .setPersisted(true)
        .apply(jobInfoFactory)
        .build()
    val jobScheduler = getSystemService<JobScheduler>()!!

    return jobScheduler.schedule(jobInfo)
}