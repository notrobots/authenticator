package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class ReplaceAccountDialog : DialogFragment() {
    var onReplaceListener: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Account already exists")
            .setMessage("An account with the same name already exists, do you wanna replace it?\n\nIf the previous account is still in use make sure to disable the 2 Factors Authentication on that account's website before overwriting this account")
            .setPositiveButton("Replace") { _, _ -> onReplaceListener() }
            .setNegativeButton("Cancel", null)
            .create()
    }
}