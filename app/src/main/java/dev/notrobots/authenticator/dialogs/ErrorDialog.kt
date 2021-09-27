package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class ErrorDialog : DialogFragment() {
    private var errorMessage: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(errorMessage)
            .setPositiveButton("Ok :(", null)
            .setCancelable(true)
            .create()
    }

    fun setErrorMessage(errorMessage: Int) {
        this.errorMessage = requireContext().getString(errorMessage)
    }

    fun setErrorMessage(errorMessage: String?) {
        this.errorMessage = errorMessage
    }
}