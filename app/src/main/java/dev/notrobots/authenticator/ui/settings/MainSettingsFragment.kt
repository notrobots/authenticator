package dev.notrobots.authenticator.ui.settings

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.preference.*
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.extensions.isDeviceSecured
import dev.notrobots.authenticator.extensions.showBiometricPrompt
import dev.notrobots.authenticator.ui.accountlist.AccountListActivity
import dev.notrobots.authenticator.ui.backupexport.ExportActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.preferences2.getHidePinsDelay
import dev.notrobots.preferences2.putAppLock

class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_main)

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val clearTextTimeoutTimePref = findPreference<EditTextPreference>(Preferences.HIDE_PINS_DELAY)
        val clearTextTimeoutTimeFormat = { text: Any ->
            //FIXME: The "s" should be replace by the current locale's variant
            val s = (text.toString().replace(Regex("^0+"), "") + "s")

            if (s.length > 1) {
                s
            } else {
                "Not set" //getString(R.string.label_unset)
            }
        }
        val appLockPref = findPreference<SwitchPreference>(Preferences.APP_LOCK)

        appLockPref?.isEnabled = requireContext().isDeviceSecured()
        appLockPref?.setOnPreferenceChangeListener { _, newValue ->
            if ((newValue as? Boolean) == true) {
                requireActivity().showBiometricPrompt(
                    getString(R.string.label_app_name),
                    null,
                    onSuccess = {
                        appLockPref.isChecked = true
                        prefs.putAppLock(true)
                    }
                )
                false
            } else {
                true
            }
        }

        findPreference<Preference>("backup_export")?.setOnPreferenceClickListener {
            requireContext().startActivity(ExportActivity::class)
            true
        }
        findPreference<Preference>("backup_import")?.setOnPreferenceClickListener {
            requireContext().startActivity(ImportActivity::class)
            true
        }

        clearTextTimeoutTimePref?.summary = clearTextTimeoutTimeFormat(prefs.getHidePinsDelay())
        clearTextTimeoutTimePref?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = clearTextTimeoutTimeFormat(newValue)
            true
        }
    }
}