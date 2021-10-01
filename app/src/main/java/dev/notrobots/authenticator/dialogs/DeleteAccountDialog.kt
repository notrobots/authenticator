package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dev.notrobots.authenticator.models.Account

class DeleteAccountDialog(
    private val accounts: List<Account>,
) : DialogFragment() {
    var onConfirmListener: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val itemNames = accounts.joinToString("<br/>") { "• ${it.displayName}" }
        val message = "You're about to remove the following accounts:<br/><br/>$itemNames<br/><br/><b>Deleting an account from the authenticator won't disable the 2FA, before deleting an account make sure to disable the 2FA on the website/service the account is tied to</b>".let {
            if (Build.VERSION.SDK_INT >= 24) {
                Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(it)
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Remove account(s)")
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove account(s)") { _, _ -> onConfirmListener() }
            .create()
    }
}