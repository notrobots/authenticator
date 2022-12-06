package dev.notrobots.authenticator.widget.preference

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialListPreferenceDialog : ListPreferenceDialogFragmentCompat() {
    private var mWhichButtonClicked = 0
    private var onDialogClosedWasCalledFromOnDismiss = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE

        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText, this)
            .setNegativeButton(preference.negativeButtonText, this)
        val contentView = onCreateDialogView(requireActivity())

        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(preference.dialogMessage)
        }

        onPrepareDialogBuilder(builder)
        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        mWhichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDialogClosedWasCalledFromOnDismiss = true
        super.onDismiss(dialog)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (onDialogClosedWasCalledFromOnDismiss) {
            onDialogClosedWasCalledFromOnDismiss = false
            super.onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
        } else {
            super.onDialogClosed(positiveResult)
        }
    }

    companion object {
        fun newInstance(key: String): MaterialListPreferenceDialog {
            val dialog = MaterialListPreferenceDialog()
            val bundle = Bundle(1)

            bundle.putString("key", key)
            dialog.arguments = bundle

            return dialog
        }

        fun show(key: String, parent: PreferenceFragmentCompat) {
            val dialog = newInstance(key)

            dialog.setTargetFragment(parent, 0)
            dialog.show(parent.parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
        }
    }
}