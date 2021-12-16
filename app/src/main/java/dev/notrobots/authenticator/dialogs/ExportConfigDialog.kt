package dev.notrobots.authenticator.dialogs

import androidx.fragment.app.DialogFragment

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