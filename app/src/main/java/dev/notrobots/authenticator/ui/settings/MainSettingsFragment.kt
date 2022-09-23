package dev.notrobots.authenticator.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.notrobots.authenticator.R

class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_main)
    }
}