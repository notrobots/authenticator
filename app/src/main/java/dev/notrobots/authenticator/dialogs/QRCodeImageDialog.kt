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
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import dev.notrobots.authenticator.models.QRCode

class QRCodeImageDialog(
    private var qrCode: QRCode? = null,
    private var contentDescription: String? = null
) : DialogFragment() {
    private lateinit var binding: DialogImageBinding

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(SAVED_STATE_QR_CODE, qrCode)
        outState.putString(SAVED_STATE_CONTENT_DESCRIPTION, contentDescription)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        binding = bindView(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            qrCode = it.getSerializable(SAVED_STATE_QR_CODE) as QRCode
            contentDescription = it.getString(SAVED_STATE_CONTENT_DESCRIPTION)
        }

        qrCode?.let {
            binding.image.setImageBitmap(it.toBitmap())
        }

        binding.image.contentDescription = contentDescription

        return MaterialDialogBuilder(requireContext())
            .setTitle(R.string.label_qr_code)
            .setView(binding.root)
            .setPositiveButton(R.string.label_close, null)
            .setNegativeButton(R.string.label_copy_uri) { d, _ ->
                qrCode?.content?.let {
                    requireContext().copyToClipboard(it)
                    requireContext().makeToast(R.string.label_uri_copied)
                }
            }
            .create()
    }

    companion object {
        private const val SAVED_STATE_QR_CODE = "QRCodeImageDialog.qrCode"
        private const val SAVED_STATE_CONTENT_DESCRIPTION = "QRCodeImageDialog.contentDescription"
    }
}