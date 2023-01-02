package dev.notrobots.authenticator.dialogs

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.PreferenceDialogBackupFrequencyBinding
import dev.notrobots.authenticator.ui.backupmanager.BackupFrequency
import dev.notrobots.authenticator.dialogs.preference.BackupFrequencyPreference

class BackupFrequencyPreferenceDialog : PreferenceDialogFragmentCompat() {
    private lateinit var binding: PreferenceDialogBackupFrequencyBinding

    override fun onCreateDialogView(context: Context): View {
        binding = bindView(context)
        return binding.root
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val toolbar = binding.toolbarLayout.toolbar
        val frequency = preference.backupFrequency

        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.title = "Backup frequency"
        toolbar.inflateMenu(R.menu.menu_dialog_preference_backup_frequency)
        toolbar.setOnMenuItemClickListener {
            dismiss()
            true
        }

        binding.days.setText(frequency?.days?.toString() ?: "0")
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        preference.backupFrequency = BackupFrequency(
            binding.days.text.toString().toInt()
        )
    }

    override fun getPreference(): BackupFrequencyPreference {
        return super.getPreference() as BackupFrequencyPreference
    }

    companion object {
        fun newInstance(preference: Preference): BackupFrequencyPreferenceDialog {
            val fragment = BackupFrequencyPreferenceDialog()
            val bundle = Bundle(1)

            bundle.putString(ARG_KEY, preference.key)
            fragment.arguments = bundle

            return fragment
        }
    }
}