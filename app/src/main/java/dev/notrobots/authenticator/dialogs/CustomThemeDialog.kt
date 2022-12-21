package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.DialogCustomThemeBinding
import dev.notrobots.authenticator.extensions.first
import dev.notrobots.authenticator.models.CustomAppTheme

class CustomThemeDialog : DialogFragment() {
    private var binding: DialogCustomThemeBinding? = null
    private var onCancelListener: DialogInterface.OnCancelListener? = null
    var theme = Enum.first<CustomAppTheme>()

    @NightMode
    var nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        set(value) {
            field = value
            binding?.theme?.check(
                when (nightMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> R.id.light_theme
                    AppCompatDelegate.MODE_NIGHT_YES -> R.id.dark_theme

                    else -> R.id.system_theme
                }
            )
        }
    var trueBlack = false
        set(value) {
            field = value
            binding?.trueBlackGroup?.check(if (trueBlack) R.id.true_black_on else R.id.true_black_off)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("theme", theme.name)
        outState.putInt("nightMode", nightMode)
        outState.putBoolean("trueBlack", trueBlack)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        binding = bindView(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            theme = parseEnum(savedInstanceState.getString("theme"))
            nightMode = savedInstanceState.getInt("nightMode")
            trueBlack = savedInstanceState.getBoolean("trueBlack")
        } else {
            nightMode = nightMode
            trueBlack = trueBlack
        }

        binding!!.theme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                nightMode = when (checkedId) {
                    R.id.light_theme -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.dark_theme -> AppCompatDelegate.MODE_NIGHT_YES

                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        }
        binding!!.trueBlackGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                trueBlack = when (checkedId) {
                    R.id.true_black_on -> true
                    else -> false
                }
            }
        }
        binding!!.themeColor.setOnClickListener {
            val dialog = CustomThemeColorDialog()

            dialog.theme = theme
            dialog.setOnSelectColorListener(object : CustomThemeColorDialog.OnSelectColorListener {
                override fun onSelect(theme: CustomAppTheme) {
                    this@CustomThemeDialog.theme = theme
                }
            })
            dialog.show(requireActivity().supportFragmentManager, null)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(null)
            .setView(binding!!.root)
            .setPositiveButton("Ok") { d, _ ->
                d.dismiss()
            }
            .setNegativeButton(R.string.label_cancel, null)
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelListener?.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onCancelListener?.onCancel(dialog)
    }

    fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener?) {
        this.onCancelListener = onCancelListener
    }
}