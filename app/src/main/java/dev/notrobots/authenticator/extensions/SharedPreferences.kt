package dev.notrobots.authenticator.extensions

import android.content.SharedPreferences
import androidx.core.content.edit

internal fun SharedPreferences.setBackupJobFirstRun(jobId: Int, firstRun: Boolean) = edit {
    putBoolean("backup_job_${jobId}_first_run", firstRun)
}

internal fun SharedPreferences.isBackupJobFirstRun(jobId: Int): Boolean {
    return getBoolean("backup_job_${jobId}_first_run", true)
}