package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.authenticator.R

class ErrorDialog(
    private var errorMessage: String? = null
) : DialogFragment() {
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(SAVED_STATE_ERROR_MESSAGE, errorMessage)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            errorMessage = it.getString(SAVED_STATE_ERROR_MESSAGE)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_error)
            .setMessage(errorMessage)
            .setPositiveButton(R.string.label_ok, null)
            .setCancelable(true)
            .create()
    }

    companion object {
        private const val SAVED_STATE_ERROR_MESSAGE = "ErrorDialog.errorMessage"
    }
}