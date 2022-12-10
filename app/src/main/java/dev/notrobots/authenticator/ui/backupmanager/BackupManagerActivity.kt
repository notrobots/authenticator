package dev.notrobots.authenticator.ui.backupmanager

import android.app.job.JobScheduler
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.androidstuff.util.Logger.Companion.logi
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
        private val notificationManager by lazy {
            NotificationManagerCompat.from(requireContext())
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
        private val localBackupFrequencyPref by lazy {
            findPreference<EditTextPreference>(Preferences.LOCAL_BACKUP_FREQUENCY)
        }
        private val localBackupPathPref by lazy {
            findPreference<Preference>(Preferences.LOCAL_BACKUP_PATH)
        }
        private val driveBackupFrequencyPref by lazy {
            findPreference<EditTextPreference>(Preferences.DRIVE_BACKUP_FREQUENCY)
        }
        private val driveBackupPathPref by lazy {
            findPreference<Preference>(Preferences.DRIVE_BACKUP_PATH)
        }
        private val localBackupNowPref by lazy {
            findPreference<Preference>("local_backup_now")
        }
        private val localBackupPref by lazy {
            findPreference<SwitchPreferenceCompat>(Preferences.LOCAL_BACKUP_ENABLED)
        }
        private val driveBackupPref by lazy {
            findPreference<SwitchPreferenceCompat>(Preferences.DRIVE_BACKUP_ENABLED)
        }
        private val logger = Logger(this)

        @Inject
        protected lateinit var accountDao: AccountDao

        @Inject
        protected lateinit var tagDao: TagDao

        @Inject
        protected lateinit var accountTagCrossRefDao: AccountTagCrossRefDao

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_backup_manager)

            localBackupPref?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    val interval = preferences.getLocalBackupFrequency().toIntOrNull()
                    val path = preferences.getLocalBackupPath()
                    val uri = path.toUri()

                    if (!requireContext().contentResolver.isPersistedPermissionGranted(uri)) {
                        requireContext().makeToast(R.string.error_local_backup_failed_no_permission)
                    } else if (path.isBlank()) {
                        requireContext().makeToast(R.string.error_path_not_set)
                    } else if (interval == null || interval <= 0) {
                        requireContext().makeToast(R.string.error_frequency_not_set)
                    } else {
                        requestNotificationPermission()

                        val scheduleResult = BackupJob.schedule<LocalBackupJob>(
                            requireContext(),
                            LocalBackupJob.JOB_ID,
                            TimeUnit.MINUTES.toMillis(15)//TODO: Use the actual value daysToMillis(interval)
                        )

                        if (scheduleResult == JobScheduler.RESULT_FAILURE) {
                            requireContext().makeToast(R.string.error_scheduling_backup_job)
                            logger.loge("Cannot schedule job")
                        } else {
                            preferences.setBackupJobFirstRun(LocalBackupJob.JOB_ID, true)
                            logger.logi("Job scheduled")
                            return@setOnPreferenceChangeListener true
                        }
                    }
                } else {
                    jobScheduler.cancel(LocalBackupJob.JOB_ID)
                    logger.logi("Job cancelled")
                    return@setOnPreferenceChangeListener true
                }

                return@setOnPreferenceChangeListener false
            }
            localBackupFrequencyPref?.setTypedSummaryProvider(::formatFrequency)
            localBackupPathPref?.setSummaryProvider(::formatLocalPath)
            localBackupPathPref?.setOnPreferenceClickListener {
                pickLocalPath.launch(null)
                true
            }
            localBackupNowPref?.setOnPreferenceClickListener {
                val interval = preferences.getLocalBackupFrequency().toIntOrNull()
                val path = preferences.getLocalBackupPath()
                val uri = path.toUri()

                if (!requireContext().contentResolver.isPersistedPermissionGranted(uri)) {
                    requireContext().makeToast(R.string.error_local_backup_failed_no_permission)
                } else if (interval == null || interval <= 0) {
                    requireContext().makeToast(R.string.error_frequency_not_set)
                } else if (path.isBlank()) {
                    requireContext().makeToast(R.string.error_path_not_set)
                } else {
                    val file = BackupManager.getLocalBackupFile(requireContext(), uri)

                    if (file != null) {
                        lifecycleScope.launch {
                            val accounts = accountDao.getAccounts()
                            val tags = tagDao.getTags()
                            val accountsWithTags = accountTagCrossRefDao.getAccountsWithTags()

                            BackupManager.performLocalBackup(requireContext(), accounts, tags, accountsWithTags, file, preferences)
                            logger.logi("Quick backup performed")
                        }
                    } else {
                        logger.loge("Quick Local Backup: DocumentFIle is null")
                    }
                }

                true
            }

//            driveBackupPref?.setOnPreferenceChangeListener { _, newValue ->
//                if (newValue as Boolean) {
//                    val interval = preferences.getDriveBackupFrequency().toIntOrNull()
//                    val path = preferences.getDriveBackupPath()
//
//                    checkBackupPathWritePermissions()
//
//                    if (path.isBlank()) {
//                        requireContext().makeToast(R.string.error_path_not_set)
//                    } else if (interval == null || interval <= 0) {
//                        requireContext().makeToast(R.string.error_frequency_not_set)
//                    } else {
//                        requestNotificationPermission()
//
//                        val scheduleResult = BackupJob.schedule<DriveBackupJob>(
//                            requireContext(),
//                            DriveBackupJob.JOB_ID,
//                            TimeUnit.MINUTES.toMillis(15)//TODO: Use the actual value daysToMillis(interval)
//                        )
//
//                        if (scheduleResult == JobScheduler.RESULT_FAILURE) {
//                            requireContext().makeToast(R.string.error_scheduling_backup_job)
//                        } else {
//                            preferences.setBackupJobFirstRun(DriveBackupJob.JOB_ID, true)
//                            return@setOnPreferenceChangeListener true
//                        }
//                    }
//                } else {
//                    jobScheduler.cancel(DriveBackupJob.JOB_ID)
//                    return@setOnPreferenceChangeListener true
//                }
//
//                return@setOnPreferenceChangeListener false
//            }
//            driveBackupFrequencyPref?.setTypedSummaryProvider(::formatFrequency)
//            driveBackupPathPref?.setOnPreferenceClickListener {
//                //Let the user pick a path on their drive
//                true
//            }

            preferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {
            when (key) {
                Preferences.LAST_LOCAL_BACKUP_TIME -> updateLastLocalBackup()
            }
        }

        override fun onResume() {
            super.onResume()

            updateLastLocalBackup()
            checkLastLocalBackupIntegrity()
            checkBackupPathWritePermissions()
        }

        /**
         * Requests the [android.Manifest.permission.POST_NOTIFICATIONS] if needed.
         *
         * This call won't do anything if the current Android version is lower than [Build.VERSION_CODES.TIRAMISU].
         */
        private fun requestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= 33 && !notificationManager.areNotificationsEnabled()) {
                notificationPermissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
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

            localBackupNowPref?.summary = if (lastLocalBackupTime > 0 && lastLocalBackupPath.isNotBlank()) {
                val date = Date(lastLocalBackupTime)
                val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.getDefault())

                "${getString(R.string.label_last_backup)}: ${dateFormat.format(date)}\n" +
                "Path: ${TextUtil.formatFileUri(lastLocalBackupPath)}"  //xxx ugly
            } else {
                getString(R.string.label_no_last_backup)
            }
        }

        private fun checkBackupPathWritePermissions() {
            val path = preferences.getLocalBackupPath().toUri()

            if (preferences.getLocalBackupEnabled() && !requireContext().contentResolver.isPersistedPermissionGranted(path)) {
                requireContext().makeToast("Permission to local backup path was revoked")
                preferences.putLocalBackupPath("")
                preferences.putLocalBackupEnabled(false)
                localBackupPref?.isChecked = false
            }
        }
    }
}
