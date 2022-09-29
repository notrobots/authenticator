package dev.notrobots.authenticator.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.ui.backupexport.ExportActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity

class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_main)

        findPreference<Preference>("backup_export")?.setOnPreferenceClickListener {
            requireContext().startActivity(ExportActivity::class)
            true
        }
        findPreference<Preference>("backup_import")?.setOnPreferenceClickListener {
            requireContext().startActivity(ImportActivity::class)
            true
        }
    }
}