package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.setClearErrorOnType
import kotlinx.android.synthetic.main.dialog_account_group.view.*

class AddAccountGroupDialog : DialogFragment() {
    var error: String? = null
        set(value) {
            field = value
            dialogView?.layout_group_name?.error = value
        }
    var onConfirmListener: (name: String) -> Unit = {}
    var dialogView: View? = null
        private set

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_account_group, null).apply {
            layout_group_name.error = error
            layout_group_name.setClearErrorOnType()
            dialogView = this
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add group")
            .setView(view)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                onConfirmListener(view.text_group_name.text.toString())
            }
        }

        return dialog
    }
}