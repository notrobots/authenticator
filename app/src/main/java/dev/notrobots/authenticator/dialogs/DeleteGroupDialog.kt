package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteGroupDialog(
    val groupCount: Int,
    val accountCount: Int
) : DialogFragment() {
    var onConfirmListener: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove groups")
            .setMessage("You are about to remove $groupCount group(s) and $accountCount account(s)")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                onConfirmListener()
            }
            .create()
    }
}