package dev.notrobots.authenticator.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.preference.*
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.extensions.isDeviceSecured
import dev.notrobots.authenticator.extensions.requestExport
import dev.notrobots.authenticator.extensions.showBiometricPrompt
import dev.notrobots.authenticator.models.AppTheme
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.authenticator.ui.backupmanager.BackupManagerActivity
import dev.notrobots.preferences2.*
import dev.notrobots.preferences2.util.parseEnum

class SettingsMainFragment : PreferenceFragmentCompat() {
    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }
    private var appLockPref: SwitchPreference? = null
    private var exportLockPref: SwitchPreference? = null
    private var dynamicColorsPref: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_settings_main)

        appLockPref = findPreference(Preferences.APP_LOCK)
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
        exportLockPref = findPreference(Preferences.EXPORT_LOCK)
        dynamicColorsPref = findPreference(Preferences.DYNAMIC_COLORS)

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

            (requireActivity() as? AuthenticatorActivity)?.setTheme(theme, dynamicColors, true)
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicColorsPref?.setOnPreferenceChangeListener { _, newValue ->
                val theme = prefs.getAppTheme<AppTheme>()
                val dynamicColors = newValue as Boolean

                (requireActivity() as? AuthenticatorActivity)?.setTheme(theme, dynamicColors, true)
                true
            }
        } else {
            dynamicColorsPref?.isEnabled = false
            dynamicColorsPref?.setSummary(R.string.label_dynamic_colors_requires_android_13)
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
    }

    override fun onResume() {
        super.onResume()

        val isDeviceSecured = requireContext().isDeviceSecured()
        val theme = prefs.getAppTheme<AppTheme>()

        exportLockPref?.isEnabled = isDeviceSecured
        appLockPref?.isEnabled = isDeviceSecured

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicColorsPref?.isEnabled = theme != AppTheme.Custom
        }
    }

    private fun formatClearTextTimeoutTime(text: Any?): String {
        val s = (text.toString().replace(Regex("^0+"), "") + getString(R.string.label_seconds_abbreviation))

        return if (s.length > 1) {
            s
        } else {
            getString(R.string.label_unset)
        }
    }
}