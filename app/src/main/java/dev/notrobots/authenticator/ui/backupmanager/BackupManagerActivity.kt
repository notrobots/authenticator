package dev.notrobots.authenticator.ui.backupmanager

import android.annotation.SuppressLint
import android.app.job.JobScheduler
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.databinding.ActivityBackupManagerBinding
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.extensions.setBackupJobFirstRun
import dev.notrobots.authenticator.services.BackupJob
import dev.notrobots.authenticator.services.DriveBackupJob
import dev.notrobots.authenticator.services.LocalBackupJob
import dev.notrobots.authenticator.ui.backupimportresult.ImportResultActivity
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.util.TextUtil
import dev.notrobots.authenticator.util.daysToMillis
import dev.notrobots.preferences2.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
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

    @AndroidEntryPoint
    class BackupManagerFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val notificationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) {
                requireContext().makeToast("Permission denied.\nYou won't get notified when a backup is performed.")
            }
        }
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
        private var lastLocalBackupPref: Preference? = null
        private val localBackupPref by lazy {
            findPreference<SwitchPreference>(Preferences.LOCAL_BACKUP_ENABLED)
        }
        private val driveBackupPref by lazy {
            findPreference<SwitchPreference>(Preferences.DRIVE_BACKUP_ENABLED)
        }
        private val localBackupNowPref by lazy {
            findPreference<Preference>("local_backup_now")
        }

        @Inject
        protected lateinit var accountDao: AccountDao

        @Inject
        protected lateinit var tagDao: TagDao

        @Inject
        protected lateinit var accountTagCrossRefDao: AccountTagCrossRefDao

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_backup_manager)

            localBackupPref?.setOnPreferenceChangeListener(::requestNotificationPermission)
            driveBackupPref?.setOnPreferenceChangeListener(::requestNotificationPermission)

            localBackupFrequencyPref = findPreference(Preferences.LOCAL_BACKUP_FREQUENCY)
            localBackupPathPref = findPreference(Preferences.LOCAL_BACKUP_PATH)
            driveBackupFrequencyPref = findPreference(Preferences.DRIVE_BACKUP_FREQUENCY)
            driveBackupPathPref = findPreference(Preferences.DRIVE_BACKUP_PATH)
            lastLocalBackupPref = findPreference("_last_local_backup")

            localBackupFrequencyPref?.setTypedSummaryProvider(::formatFrequency)
            localBackupPathPref?.setSummaryProvider(::formatLocalPath)
            localBackupPathPref?.setOnPreferenceClickListener {
                pickLocalPath.launch(null)
                true
            }
            driveBackupFrequencyPref?.setTypedSummaryProvider(::formatFrequency)
            driveBackupPathPref?.setOnPreferenceClickListener {
                //Let the user pick a path on their drive
                true
            }
            lastLocalBackupPref?.setOnPreferenceClickListener {
                if (checkLastLocalBackupIntegrity()) {
                    val uri = preferences.getLastLocalBackupPath().toUri()
                    val stream = requireContext().contentResolver.openInputStream(uri)

                    stream?.bufferedReader()?.use {
                        val data = BackupManager.importText(it.readText())

                        ImportResultActivity.showResults(requireContext(), data)
                    }
                } else {
                    requireContext().makeToast(R.string.error_backup_not_found)
                    //TODO: Ask the user if they want to remove the entry
                }

                true
            }

            updateLastLocalBackup()
            checkLastLocalBackupIntegrity()

            localBackupNowPref?.setOnPreferenceClickListener {
                val interval = preferences.getLocalBackupFrequency().toIntOrNull()
                val path = preferences.getLocalBackupPath()

                if (interval == null || interval <= 0) {
                    requireContext().makeToast(R.string.error_frequency_not_set)
                } else if (path.isBlank()) {
                    requireContext().makeToast(R.string.error_path_not_set)
                } else {
                    val directoryPath = preferences.getLocalBackupPath().toUri()
                    val file = BackupManager.getLocalBackupFile(requireContext(), directoryPath)

                    if (file != null) {
                        lifecycleScope.launch {
                            val accounts = accountDao.getAccounts()
                            val tags = tagDao.getTags()
                            val accountsWithTags = accountTagCrossRefDao.getAccountsWithTags()

                            BackupManager.performLocalBackup(requireContext(), accounts, tags, accountsWithTags, file, preferences)
                        }
                    }
                }

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
                        TimeUnit.MINUTES.toMillis(15), //TODO: Use the actual value daysToMillis(interval),
                        preferences
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
                        daysToMillis(interval),
                        preferences
                    ) {
                        requireContext().makeToast(R.string.error_scheduling_backup_job)
                        preferences.putDriveBackupEnabled(false)
                    }

                    return@setOnPreferenceChangeListener true
                }

                return@setOnPreferenceChangeListener false
            }

            preferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {
            if (key == Preferences.LAST_LOCAL_BACKUP_TIME) {
                updateLastLocalBackup()
            }
        }

        private inline fun <reified T> scheduleOrCancelJob(
            schedule: Boolean,
            id: Int,
            interval: Long,
            sharedPreferences: SharedPreferences,
            onFailure: () -> Unit
        ) {
            if (schedule) {
                val scheduleResult = BackupJob.schedule<T>(requireContext(), id, interval)

                if (scheduleResult == JobScheduler.RESULT_FAILURE) {
                    onFailure()
                } else {
                    sharedPreferences.setBackupJobFirstRun(id, true)
                }
            } else {
                jobScheduler.cancel(id)
            }
        }

        private fun requestNotificationPermission(preference: Preference, value: Any): Boolean {
            if (value is Boolean && value) {
                notificationPermissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }

            return true
        }

        private fun formatFrequency(preference: EditTextPreference): String? {
            val days = preference.text?.toIntOrNull() ?: 0
            val daysLabel = resources.getQuantityString(R.plurals.label_days, days)

            return if (days > 0) {
                "$days $daysLabel"
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

        private fun checkLastLocalBackupIntegrity(): Boolean {
            val uri = preferences.getLastLocalBackupPath().toUri()
            val time = preferences.getLastLocalBackupTime()

            if (Uri.EMPTY != uri && time > 0) {
                val document = DocumentFile.fromTreeUri(requireContext(), uri)

                return document?.exists() == true
            } else {
                preferences.putLastLocalBackupPath("")
                preferences.putLastLocalBackupTime(0)

                return false
            }
        }

        private fun updateLastLocalBackup() {
            val lastLocalBackupTime = preferences.getLastLocalBackupTime()
            val lastLocalBackupPath = preferences.getLastLocalBackupPath()

            lastLocalBackupPref?.isEnabled = lastLocalBackupTime > 0 && lastLocalBackupPath.isNotBlank()
            lastLocalBackupPref?.summary = if (lastLocalBackupTime > 0 && lastLocalBackupPath.isNotBlank()) {
                val date = Date(lastLocalBackupTime)
                val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.getDefault())

                "${getString(R.string.label_last_backup)}: ${dateFormat.format(date)}"
            } else {
                getString(R.string.label_no_last_backup)
            }
        }
    }
}
