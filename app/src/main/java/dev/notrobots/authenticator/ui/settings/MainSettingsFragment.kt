package dev.notrobots.authenticator.ui.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.ui.backupexport.ExportActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.preferences2.getHidePinsDelay

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