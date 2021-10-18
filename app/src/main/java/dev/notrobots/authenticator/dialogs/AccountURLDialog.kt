package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.inflate
import dev.notrobots.authenticator.extensions.setClearErrorOnType
import dev.notrobots.authenticator.extensions.toUri
import dev.notrobots.authenticator.models.Account
import kotlinx.android.synthetic.main.dialog_account_url.view.*

class AccountURLDialog : DialogFragment() {
    var dialogView: View? = null
    var onConfirmListener: (String) -> Unit = {}
    var onCancelListener: () -> Unit = {}
    var error: String? = null
        set(value) {
            field = value
            dialogView?.layout_account_url?.error = value
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_account_url).apply {
            layout_account_url.error = error
            layout_account_url.setClearErrorOnType()
            dialogView = this
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add account")
            .setView(view)
            .setCancelable(true)
            .setPositiveButton("Ok", null)
            .setNeutralButton("Cancel") { _, _ -> onCancelListener() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                val text = view.text_account_url.text.toString()

                if (text.isBlank()) {
                    view.layout_account_url.error = "Field is empty"
                } else {
                    onConfirmListener(text)
                }
            }
        }

        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelListener()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onCancelListener()
    }
}