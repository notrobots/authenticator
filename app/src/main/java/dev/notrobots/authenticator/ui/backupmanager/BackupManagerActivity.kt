package dev.notrobots.authenticator.ui.backupmanager

import android.app.job.JobScheduler
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.*
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.databinding.ActivityBackupManagerBinding
import dev.notrobots.authenticator.extensions.schedulePeriodicJob
import dev.notrobots.authenticator.extensions.setCustomSummaryProvider
import dev.notrobots.authenticator.extensions.updateSummary
import dev.notrobots.authenticator.services.DriveBackupJob
import dev.notrobots.authenticator.services.LocalBackupJob
import dev.notrobots.authenticator.util.TextUtil
import dev.notrobots.authenticator.util.daysToMillis
import dev.notrobots.preferences2.*
import java.util.concurrent.TimeUnit

class BackupManagerActivity : AuthenticatorActivity() {
    private val binding by viewBindings<ActivityBackupManagerBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        finishOnBackPressEnabled = true
        setContentView(binding.root)

        supportFragmentManager
            .beginTransaction()
            .replace(binding.container.id, BackupManagerFragment())
            .commit()
    }

    class BackupManagerFragment : PreferenceFragmentCompat() {
        private val pickLocalPath = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            it?.let {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                //TODO: Let users revoke this or revoke it when changing path
                requireContext().contentResolver.takePersistableUriPermission(it, flags)

                preferences.putLocalBackupPath(it.toString())
                localBackupPathPref?.updateSummary()
            }
        }
        private val jobScheduler by lazy {
            requireContext().getSystemService(JobScheduler::class.java)
        }
        private val preferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        }
        private var localBackupFrequencyPref: EditTextPreference? = null
        private var localBackupPathPref: Preference? = null
        private var driveBackupFrequencyPref: EditTextPreference? = null
        private var driveBackupPathPref: Preference? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_backup_manager)

            localBackupFrequencyPref = findPreference(Preferences.LOCAL_BACKUP_FREQUENCY)
            localBackupPathPref = findPreference(Preferences.LOCAL_BACKUP_PATH)
            driveBackupFrequencyPref = findPreference(Preferences.DRIVE_BACKUP_FREQUENCY)
            driveBackupPathPref = findPreference(Preferences.DRIVE_BACKUP_PATH)

            localBackupFrequencyPref?.setCustomSummaryProvider(::formatFrequency)
            localBackupPathPref?.setSummaryProvider(::formatLocalPath)
            localBackupPathPref?.setOnPreferenceClickListener {
                pickLocalPath.launch(null)
                true
            }
            driveBackupFrequencyPref?.setCustomSummaryProvider(::formatFrequency)
            driveBackupPathPref?.setOnPreferenceClickListener {
                //Let the user pick a path on their drive
                true
            }

            findPreference<SwitchPreference>(Preferences.LOCAL_BACKUP_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
                val interval = preferences.getLocalBackupFrequency().toIntOrNull()
                val path = preferences.getLocalBackupPath()

                if (interval == null || interval <= 0) {
                    requireContext().makeToast(R.string.error_frequency_not_set)
                } else if (path.isBlank()) {
                    requireContext().makeToast(R.string.error_path_not_set)
                } else {
                    scheduleOrCancelJob<LocalBackupJob>(
                        newValue == true,
                        LocalBackupJob.JOB_ID,
                        TimeUnit.MINUTES.toMillis(15) ?: daysToMillis(interval)
                    ) {
                        requireContext().makeToast(R.string.error_scheduling_backup_job)
                        preferences.putLocalBackupEnabled(false)
                    }

                    return@setOnPreferenceChangeListener true
                }

                return@setOnPreferenceChangeListener false
            }

            findPreference<SwitchPreference>(Preferences.DRIVE_BACKUP_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
                val interval = preferences.getDriveBackupFrequency().toIntOrNull()
                val path = preferences.getDriveBackupPath()

                if (interval == null || interval <= 0) {
                    requireContext().makeToast(R.string.error_frequency_not_set)
                } else if (path.isBlank()) {
                    requireContext().makeToast(R.string.error_path_not_set)
                } else {
                    scheduleOrCancelJob<DriveBackupJob>(
                        newValue == true,
                        DriveBackupJob.JOB_ID,
                        daysToMillis(interval)
                    ) {
                        requireContext().makeToast(R.string.error_scheduling_backup_job)
                        preferences.putDriveBackupEnabled(false)
                    }

                    return@setOnPreferenceChangeListener true
                }

                return@setOnPreferenceChangeListener false
            }
        }

        private inline fun <reified T> scheduleOrCancelJob(schedule: Boolean, id: Int, interval: Long, onFailure: () -> Unit) {
            if (schedule) {
                val scheduleResult = requireContext().schedulePeriodicJob<T>(id, interval)

                if (scheduleResult == JobScheduler.RESULT_FAILURE) {
                    onFailure()
                }
            } else {
                jobScheduler.cancel(id)
            }
        }

        private fun formatFrequency(preference: EditTextPreference): String? {
            val days = preference.text.toInt()
            val s = "$days ${resources.getQuantityString(R.plurals.label_days, days)}"

            return if (s.length > 5) {
                s
            } else {
                getString(R.string.label_unset)
            }
        }

        private fun formatLocalPath(preference: Preference): String? {
            val path = preferences.getLocalBackupPath("")

            return if (path.isNotBlank()) {
                TextUtil.formatFileUri(path)
            } else {
                getString(R.string.label_unset)
            }
        }
    }
}
