package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.ExportFormat
import kotlinx.android.synthetic.main.dialog_export_config.view.*

class ExportConfigDialog : DialogFragment() {
//    var onExportListener: (format: ExportFormat, compression: ExportCompression) -> Unit = { _, _ -> }
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val view = layoutInflater.inflate(R.layout.dialog_export_config, null).apply {
//            spinner_export_format.values = ExportFormat.values().toList()
//            spinner_export_compression.values = ExportCompression.values().toList()
//        }
//        val dialog = MaterialAlertDialogBuilder(requireContext())
//            .setTitle("Export")
//            .setView(view)
//            .setNegativeButton("Cancel", null)
//            .setPositiveButton("Export") { _, _ ->
//                onExportListener(
//                    view.spinner_export_format.selectedValue as ExportFormat,
//                    view.spinner_export_compression.selectedValue as ExportCompression
//                )
//            }
//            .create()
//
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//
//        return dialog
//    }
}