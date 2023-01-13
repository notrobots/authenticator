package dev.notrobots.authenticator.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.databinding.ActivitySettingsBinding
import dev.notrobots.authenticator.dialogs.CustomThemeDialog
import dev.notrobots.authenticator.dialogs.preference.CountdownIndicatorPreferenceDialog
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.extensions.requestExport
import dev.notrobots.authenticator.extensions.showBiometricPrompt
import dev.notrobots.authenticator.models.AppTheme
import dev.notrobots.authenticator.models.TotpClock
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.authenticator.ui.backupmanager.BackupManagerActivity
import dev.notrobots.authenticator.util.NetworkTimeProvider
import dev.notrobots.authenticator.widget.preference.CountdownIndicatorPreference
import dev.notrobots.preferences2.*
import dev.notrobots.preferences2.fragments.MaterialPreferenceFragment
import dev.notrobots.preferences2.util.parseEnum
import java.util.prefs.PreferenceChangeEvent
import java.util.prefs.PreferenceChangeListener
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class SettingsActivity : AuthenticatorActivity() {
    private val binding: ActivitySettingsBinding by viewBindings()
    private val toolbar by lazy {
        binding.toolbarLayout.toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        finishOnBackPressEnabled = true
        setContentView(binding.root)

        supportFragmentManager
            .beginTransaction()
            .replace(binding.container.id, SettingsFragment())
            .commit()
    }

    @AndroidEntryPoint
    class SettingsFragment : MaterialPreferenceFragment() {
        private val prefs by lazy {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        }
        private val authenticatorActivity by lazy {
            requireActivity() as? AuthenticatorActivity
        }
        private val appLockPref by lazy {
            findPreference<SwitchPreferenceCompat>(Preferences.APP_LOCK)
        }
        private val exportLockPref by lazy {
            findPreference<SwitchPreferenceCompat>(Preferences.EXPORT_LOCK)
        }
        private val dynamicColorsPref by lazy {
            findPreference<SwitchPreferenceCompat>(Preferences.DYNAMIC_COLORS)
        }
        private val customAppThemePref by lazy {
            findPreference<Preference>("_custom_app_theme")
        }
        private val timeSyncPref by lazy {
            findPreference<Preference>("time_sync")
        }

        @Inject
        protected lateinit var totpClock: TotpClock

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_settings)

            appLockPref?.setOnPreferenceChangeListener { _, newValue ->
                if ((newValue as? Boolean) == true) {
                    requireActivity().showBiometricPrompt(
                        getString(R.string.label_app_name),
                        null,
                        onSuccess = {
                            appLockPref?.isChecked = true
                            prefs.putAppLock(true)
                        }
                    )
                    false
                } else {
                    true
                }
            }
            customAppThemePref?.setOnPreferenceClickListener {
                CustomThemeDialog(
                    prefs.getCustomAppTheme(),
                    prefs.getCustomAppThemeNightMode(),
                    prefs.getCustomAppThemeTrueBlack()
                ).show(requireActivity().supportFragmentManager, null)

                true
            }
            customAppThemePref?.isVisible = prefs.getAppTheme<AppTheme>() == AppTheme.Custom
            dynamicColorsPref?.setOnPreferenceChangeListener { _, newValue ->
                val theme = prefs.getAppTheme<AppTheme>()
                val dynamicColors = newValue as Boolean

                (requireActivity() as? AuthenticatorActivity)?.setTheme(theme, dynamicColors, true)
                true
            }
            dynamicColorsPref?.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            findPreference<Preference>("backup_export")?.setOnPreferenceClickListener {
                requireActivity().requestExport(
                    prefs.getExportLock(),
                    requireContext().isDeviceSecured()
                )
                true
            }

            findPreference<Preference>("backup_import")?.setOnPreferenceClickListener {
                requireContext().startActivity(ImportActivity::class)
                true
            }

            findPreference<Preference>(Preferences.APP_THEME)?.setOnPreferenceChangeListener { _, newValue ->
                val theme = parseEnum<AppTheme>(newValue.toString(), true)
                val dynamicColors = prefs.getDynamicColors()

                authenticatorActivity?.setTheme(theme, dynamicColors, true)
                true
            }

            findPreference<Preference>("backup_manager")?.setOnPreferenceClickListener {
                requireContext().startActivity(BackupManagerActivity::class)
                true
            }

            findPreference<Preference>("app_version")?.setSummaryProvider {
                try {
                    val pInfo = if (Build.VERSION.SDK_INT >= 33) {
                        requireContext().packageManager.getPackageInfo(requireContext().packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                    }

                    pInfo.versionName.toString()
                } catch (e: Exception) {
                    "null"
                }
            }

            timeSyncPref?.setOnPreferenceClickListener {
                val timeCorrection = totpClock.getTimeCorrection()
                val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Time sync")

                NetworkTimeProvider.getTimeCorrection(
                    onSuccess = {
                        if (it == timeCorrection) {
                            dialogBuilder.setMessage("Time already synced")
                                .setPositiveButton("Ok", null)
                                .create()
                                .show()
                        } else {
                            totpClock.setTimeCorrection(it)
                            updateTimeCorrectionSummary()

                            dialogBuilder.setMessage("Time synced correctly")
                                .setPositiveButton("Ok") { _, _ ->
                                }
                                .create()
                                .show()
                        }
                    },
                    onFailure = {
                        dialogBuilder.setMessage("Cannot sync time. Check your internet connection and try again")
                            .setPositiveButton("Ok", null)
                            .create()
                            .show()
                    }
                )

                true
            }
            updateTimeCorrectionSummary()

            setFragmentResultListener<CustomThemeDialog> { _, _ ->
                authenticatorActivity?.updateTheme(true)
            }
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (preference is CountdownIndicatorPreference) {
                CountdownIndicatorPreferenceDialog::class
                    .newInstance(preference.key)
                    .show(this)
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }

        override fun onResume() {
            super.onResume()

            val isDeviceSecured = requireContext().isDeviceSecured()
            val theme = prefs.getAppTheme<AppTheme>()

            exportLockPref?.isEnabled = isDeviceSecured
            appLockPref?.isEnabled = isDeviceSecured
            dynamicColorsPref?.isEnabled = theme != AppTheme.Custom
            customAppThemePref?.isVisible = theme == AppTheme.Custom
        }

        private fun updateTimeCorrectionSummary() {
            val timeCorrection = totpClock.getTimeCorrection()

            timeSyncPref?.summary = if (timeCorrection == 0) {
                getString(R.string.label_time_sync_never)
            } else if (timeCorrection < 0) {
                getString(R.string.label_time_sync_ahead, abs(timeCorrection))
            } else if (timeCorrection > 0) {
                getString(R.string.label_time_sync_behind, timeCorrection)
            } else null
        }
    }
}