package dev.notrobots.authenticator.ui.settings

import android.os.Bundle
import androidx.preference.*
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.extensions.isDeviceSecured
import dev.notrobots.authenticator.extensions.requestExport
import dev.notrobots.authenticator.extensions.showBiometricPrompt
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.preferences2.getExportLock
import dev.notrobots.preferences2.getHidePinsDelay
import dev.notrobots.preferences2.putAppLock

class MainSettingsFragment : PreferenceFragmentCompat() {
    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }
    private var clearTextTimeoutTimePref: EditTextPreference? = null
    private var appLockPref: SwitchPreference? = null
    private var exportLockPref: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_main)

        clearTextTimeoutTimePref = findPreference(Preferences.HIDE_PINS_DELAY)
        clearTextTimeoutTimePref?.summary = formatClearTextTimeoutTime(prefs.getHidePinsDelay())
        clearTextTimeoutTimePref?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = formatClearTextTimeoutTime(newValue)
            true
        }
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
    }

    override fun onResume() {
        super.onResume()

        val isDeviceSecured = requireContext().isDeviceSecured()

        exportLockPref?.isEnabled = isDeviceSecured
        appLockPref?.isEnabled = isDeviceSecured
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