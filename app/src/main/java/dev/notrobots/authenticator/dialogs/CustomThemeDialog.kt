package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.PreferenceManager
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.DialogCustomThemeBinding
import dev.notrobots.authenticator.extensions.first
import dev.notrobots.authenticator.extensions.setFragmentResult
import dev.notrobots.authenticator.extensions.setFragmentResultListener
import dev.notrobots.authenticator.models.CustomAppTheme
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import dev.notrobots.preferences2.putCustomAppTheme
import dev.notrobots.preferences2.putCustomAppThemeNightMode
import dev.notrobots.preferences2.putCustomAppThemeTrueBlack

class CustomThemeDialog(
    var theme: CustomAppTheme = Enum.first(),
    @NightMode
    var nightMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    var trueBlack: Boolean = false
) : DialogFragment() {
    private var binding: DialogCustomThemeBinding? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(SAVED_STATE_THEME, theme.name)
        outState.putInt(SAVED_STATE_NIGHT_MODE, nightMode)
        outState.putBoolean(SAVED_STATE_TRUE_BLACK, trueBlack)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener<CustomThemeColorDialog> { _, bundle ->
            if (bundle.containsKey(CustomThemeColorDialog.EXTRA_THEME)) {
                theme = bundle.getSerializable(CustomThemeColorDialog.EXTRA_THEME) as CustomAppTheme
                updateSelectedTheme()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        binding = bindView(requireContext())    //TODO: Find out why it's not possible to use onAttach anymore

        if (savedInstanceState != null) {
            theme = parseEnum(savedInstanceState.getString(SAVED_STATE_THEME))
            nightMode = savedInstanceState.getInt(SAVED_STATE_NIGHT_MODE)
            trueBlack = savedInstanceState.getBoolean(SAVED_STATE_TRUE_BLACK)
        }

        updateSelectedTheme()
        binding!!.theme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                nightMode = when (checkedId) {
                    R.id.light_theme -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.dark_theme -> AppCompatDelegate.MODE_NIGHT_YES

                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        }
        binding!!.trueBlackGroup.check(if (trueBlack) R.id.true_black_on else R.id.true_black_off)
        binding!!.trueBlackGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                trueBlack = when (checkedId) {
                    R.id.true_black_on -> true
                    else -> false
                }
            }
        }
        binding!!.themeColor.setOnClickListener {
            CustomThemeColorDialog(theme)
                .show(parentFragmentManager, null)
        }

        return MaterialDialogBuilder(requireContext())
            .setTitle(null)
            .setView(binding!!.root)
            .setPositiveButton(R.string.label_ok) { d, i ->
                preferences.putCustomAppTheme(theme)
                preferences.putCustomAppThemeNightMode(nightMode)
                preferences.putCustomAppThemeTrueBlack(trueBlack)
                d.dismiss()
                setFragmentResult<CustomThemeDialog>()
            }
            .setNegativeButton(R.string.label_cancel, null)
            .create()
    }

    private fun updateSelectedTheme() {
        binding?.theme?.check(
            when (nightMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.light_theme
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.dark_theme

                else -> R.id.system_theme
            }
        )
    }

    companion object {
        private const val SAVED_STATE_THEME = "CustomThemeDialog.theme"
        private const val SAVED_STATE_NIGHT_MODE = "CustomThemeDialog.nightMode"
        private const val SAVED_STATE_TRUE_BLACK = "CustomThemeDialog.trueBlack"
    }
}