package dev.notrobots.authenticator.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.preference.*
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.databinding.ActivitySettingsBinding
import dev.notrobots.authenticator.dialogs.CustomThemeDialog
import dev.notrobots.authenticator.extensions.isDeviceSecured
import dev.notrobots.authenticator.extensions.requestExport
import dev.notrobots.authenticator.extensions.showBiometricPrompt
import dev.notrobots.authenticator.models.AppTheme
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.authenticator.ui.backupmanager.BackupManagerActivity
import dev.notrobots.authenticator.widget.preference.MaterialListPreferenceDialog
import dev.notrobots.preferences2.*
import dev.notrobots.preferences2.util.parseEnum

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

    class SettingsFragment : PreferenceFragmentCompat() {
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
                val dialog = CustomThemeDialog()

                dialog.theme = prefs.getCustomAppTheme()
                dialog.nightMode = prefs.getCustomAppThemeNightMode()
                dialog.trueBlack = prefs.getCustomAppThemeTrueBlack()
                dialog.setOnCancelListener {
                    prefs.putCustomAppTheme(dialog.theme)
                    prefs.putCustomAppThemeNightMode(dialog.nightMode)
                    prefs.putCustomAppThemeTrueBlack(dialog.trueBlack)
                    authenticatorActivity?.updateTheme(true)
                }
                dialog.show(requireActivity().supportFragmentManager, null)

                true
            }
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
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (preference is ListPreference) {
                MaterialListPreferenceDialog.show(preference.key, this)
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
    }
}