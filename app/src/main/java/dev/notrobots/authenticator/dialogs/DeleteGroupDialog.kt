package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.getText

class DeleteGroupDialog(
    private val groupCount: Int,
    private val accountCount: Int,
    private val onConfirm: () -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = resources.getText(
            R.string.label_delete_group_2,
            resources.getQuantityString(R.plurals.label_quantity_group, groupCount, groupCount),
            resources.getQuantityString(R.plurals.label_quantity_account, accountCount, accountCount)
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getQuantityString(R.plurals.label_delete_group_1, groupCount, groupCount))
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ -> onConfirm() }
            .create()
    }
}