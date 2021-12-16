package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dev.notrobots.authenticator.models.BackupFormat
import dev.notrobots.authenticator.util.adapterOf

class ExportFormatDialog : DialogFragment() {
    var onTypeSelectListener: (format: BackupFormat) -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val values = BackupFormat.values()
        val names = values.map { it.name }
        val adapter = adapterOf(requireContext(), names)

        return AlertDialog.Builder(requireContext())
            .setTitle("Select an export format")
            .setAdapter(adapter) { _, i ->
                onTypeSelectListener(values[i])
            }
            .create()
    }
}