package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.androidstuff.extensions.setClearErrorOnType
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.DialogAccountUriBinding
import dev.notrobots.authenticator.extensions.inflate

class AddAccountUriDialog(
    fragmentManager: FragmentManager,
    var onConfirmListener: (String, AddAccountUriDialog) -> Unit
) : InstantDialog(fragmentManager) {
    var error: String? = null
        set(value) {
            field = value
            binding?.layoutAccountUrl?.error = value
        }
    var binding: DialogAccountUriBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAccountUriBinding.bind(layoutInflater.inflate(R.layout.dialog_account_uri))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add account")
            .setView(binding!!.root)
            .setPositiveButton(R.string.label_ok, null)
            .setNeutralButton(R.string.label_cancel, null)
            .create()

        binding!!.layoutAccountUrl.error = error
        binding!!.layoutAccountUrl.setClearErrorOnType()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                val text = binding!!.textAccountUrl.text.toString()

                if (text.isBlank()) {
                    binding!!.layoutAccountUrl.error = "Field is empty"
                } else {
                    onConfirmListener(text, this)
                }
            }
        }

        return dialog
    }
}