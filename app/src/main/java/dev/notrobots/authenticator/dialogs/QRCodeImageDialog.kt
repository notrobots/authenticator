package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.DialogImageBinding
import dev.notrobots.authenticator.models.QRCode

//FIXME DialogFragment and Fragment subclasses cannot have constructor parameters or the system won't be able to instantiate them
class QRCodeImageDialog private constructor() : DialogFragment() {
    private lateinit var binding: DialogImageBinding
    private var qrCode: QRCode? = null
    private var contentDescription: String? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        saveToBundle(outState, qrCode, contentDescription)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        binding = bindView(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            loadFromBundle(savedInstanceState, this)
        } else {
            arguments?.let { loadFromBundle(it, this) }
        }

        qrCode?.let {
            binding.image.setImageBitmap(it.toBitmap())
        }

        binding.image.contentDescription = contentDescription

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_qr_code)
            .setView(binding.root)
            .setPositiveButton(R.string.label_close, null)
            .setNegativeButton(R.string.label_copy_uri, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener {
                        qrCode?.content?.let {
                            requireContext().copyToClipboard(it)
                            requireContext().makeToast(R.string.label_uri_copied)
                        }
                    }
                }
                show()
            }
    }

    companion object {
        fun newInstance(qrCode: QRCode?, contentDescription: String?): DialogFragment {
            val dialog = QRCodeImageDialog()
            val bundle = Bundle()

            saveToBundle(bundle, qrCode, contentDescription)
            dialog.arguments = bundle

            return dialog
        }

        fun loadFromBundle(bundle: Bundle, dialog: QRCodeImageDialog) {
            dialog.qrCode = bundle.getSerializable("qrCode") as QRCode
            dialog.contentDescription = bundle.getString("contentDescription")
        }

        fun saveToBundle(bundle: Bundle, qrCode: QRCode?, contentDescription: String?) {
            bundle.putSerializable("qrCode", qrCode)
            bundle.putString("contentDescription", contentDescription)
        }
    }
}