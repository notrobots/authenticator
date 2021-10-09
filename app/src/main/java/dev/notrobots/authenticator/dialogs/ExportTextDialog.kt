package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class ExportTextDialog(
    private val message: String,
) : DialogFragment() {
    private var listener: Listener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Export accounts")
            .setMessage(message)
            .setPositiveButton("Save") { _, _ ->
                listener?.onSave()
            }
            .setNegativeButton("Cancel") { _, _ ->
                listener?.onCopy()
            }
            .setNeutralButton("Copy") { _, _ ->
                listener?.onClose()
            }
            .create()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun onSave()
        fun onCopy()
        fun onClose()
    }
}