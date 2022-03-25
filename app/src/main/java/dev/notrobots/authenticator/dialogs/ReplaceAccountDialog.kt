package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.authenticator.R

class ReplaceAccountDialog(
    fragmentManager: FragmentManager,
    private val listener: Listener
) : InstantDialog(fragmentManager) {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_account_already_exists_title)
            .setMessage(R.string.label_account_already_exists_description)
            .setPositiveButton(R.string.label_replace) { _, _ -> listener.onReplace() }
            .setNeutralButton(R.string.label_keep_both) { _,_ -> listener.onKeepBoth() }
            .setNegativeButton(R.string.label_cancel, null)
            .create()
    }

    interface Listener {
        fun onReplace()
        fun onKeepBoth()
    }
}