package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
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
    private var errorView: TextInputLayout? = null
    var onConfirmListener: (Account) -> Unit = {}
    var onCancelListener: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_account_url).apply {
            errorView = layout_account_url
            layout_account_url.setClearErrorOnType()
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
                val text = view.text_account_url.text

                if (text.isNullOrBlank()) {
                    view.layout_account_url.error = "Field is empty"
                } else {
                    try {
                        val account = Account.parse(text.toUri())

                        onConfirmListener(account)

                        // Dialog is only dismissed if no exception is thrown
                        // or if the user clicks outside or on the neutral button
                        dialog.dismiss()
                    } catch (e: Exception) {
                        view.layout_account_url.error = e.message
                    }
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