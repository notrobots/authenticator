package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.androidstuff.extensions.resolveString
import dev.notrobots.androidstuff.extensions.setClearErrorOnType
import dev.notrobots.androidstuff.extensions.setError
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.DialogAccountUriBinding
import dev.notrobots.authenticator.extensions.inflate

class AccountUriDialog(
    fragmentManager: FragmentManager,
    val title: Any? = null,
    var onConfirmListener: (String, AccountUriDialog) -> Unit
) : InstantDialog(fragmentManager) {
    private var binding: DialogAccountUriBinding? = null
    var error: String? = null
        set(value) {
            field = value
            binding?.layoutAccountUrl?.error = value
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAccountUriBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(context?.resolveString(title))
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
                    binding!!.layoutAccountUrl.setError(R.string.error_empty_field)
                } else {
                    onConfirmListener(text, this)
                }
            }
        }

        return dialog
    }
}