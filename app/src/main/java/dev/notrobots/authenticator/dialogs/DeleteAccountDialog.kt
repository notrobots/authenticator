package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.getQuantityText

class DeleteAccountDialog(
    private val accountCount: Int,
    private val onConfirm: () -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = resources.getQuantityText(R.plurals.label_delete_account_title, accountCount, accountCount)
        val message = resources.getQuantityText(R.plurals.label_delete_account_desc, accountCount, accountCount)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(title) { _, _ -> onConfirm() }
            .create()
    }
}