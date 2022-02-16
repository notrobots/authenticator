package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.getQuantityText

class DeleteAccountDialog(
    private val accountCount: Int,
    fragmentManager: FragmentManager,
    private val onConfirm: () -> Unit
) : InstantDialog(fragmentManager) {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = resources.getQuantityText(
            R.plurals.label_delete_account_title,
            accountCount,
            accountCount
        )
        val message = resources.getQuantityText(
            R.plurals.label_delete_account_description,
            accountCount,
            accountCount
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.label_cancel, null)
            .setPositiveButton(title) { _, _ -> onConfirm() }
            .create()
    }
}