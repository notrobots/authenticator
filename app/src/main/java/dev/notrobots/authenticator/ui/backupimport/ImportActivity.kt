package dev.notrobots.authenticator.ui.backupimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.*
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityImportBinding
import dev.notrobots.authenticator.dialogs.AccountUriDialog
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.ui.backupimportresult.ImportResultActivity
import dev.notrobots.authenticator.util.AccountExporter

@AndroidEntryPoint
class ImportActivity : AuthenticatorActivity() {
    private val scanner by lazy {
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC
            )
            .build()

        BarcodeScanning.getClient(scannerOptions)
    }
    private val barcodeScanner = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                val uris = it.data!!.getStringArrayListExtra(BarcodeScannerActivity.EXTRA_QR_LIST) ?: listOf<String>()
                val importedData = mutableListOf<AccountExporter.ImportedData>()
                val errors = mutableListOf<String>()

                for ((index, uri) in uris.withIndex()) {
                    try {
                        val data = AccountExporter.import(uri)

                        importedData.add(data)
                    } catch (e: Exception) {
                        errors.add("QR #${index + 1}: ${e.message ?: "Invalid data"}")
                    }
                }

                if (importedData.isEmpty()) {
                    showInfo("Error", "None of the QR codes were valid")
                } else {
                    if (errors.isNotEmpty()) {
                        showChoice(
                            "Error",
                            "There were some errors while importing the data:\n\n${errors.joinToString("\n")}\n\nDo you still want to proceed? Invalid data won't be imported",
                            positiveButton = "Yes",
                            positiveCallback = {
                                showResults(importedData)
                            },
                            negativeButton = "No"
                        )
                    } else {
                        showResults(importedData)
                    }
                }
            }
        }
    }
    private val filePicker = registerForActivityResult(OpenDocument()) {
        // If the user goes back while picking a file the uri will be null
        if (it != null) {
            val mimeType = contentResolver.getType(it) ?: ""

            when {
                mimeType.startsWith("image/") -> {
                    val image = InputImage.fromFilePath(this, it)

                    scanner.process(image)
                        .addOnSuccessListener {
                            val content = it.first().rawValue

                            try {
                                val data = AccountExporter.import(content!!)

                                showResults(data)
                            } catch (e: Exception) {
                                loge("Cannot import file: ${e.message}")
                                showInfo("Error", "Import data is corrupt")//FIXME: Show detailed error with account index ecc
                            }
                        }
                        .addOnFailureListener {
                            makeSnackBar("Error scanning QR", binding.root)
                        }
                }
                mimeType.startsWith("text/") -> {
                    contentResolver.openInputStream(it)?.let {
                        val content = it.reader().readText()

                        try {
                            val data = AccountExporter.import(content)

                            showResults(data)
                        } catch (e: Exception) {
                            loge("Cannot import file: ${e.message}")
                            showInfo("Error", "Import data is corrupt")
                        }
                    }
                }

                //TODO: Json mime type

                else -> makeSnackBar("Invalid file type", binding.root)
            }
        }
    }
    private val filePickerTypes = arrayOf(
        "image/*",  //TODO: Specify the image type
        "text/*"
    )
    private val binding by viewBindings<ActivityImportBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener { finish() }

        binding.options.addOption(
            R.string.label_qr_code,
            R.string.label_scan_one_or_multiple_qr_codes,
            R.drawable.ic_qr
        ) {
            val intent = Intent(this, BarcodeScannerActivity::class.java).apply {
                putExtra(BarcodeScannerActivity.EXTRA_MULTI_SCAN, true)
            }

            barcodeScanner.launch(intent)
        }
        binding.options.addOption(
            R.string.label_file_import_title,
            R.string.label_file_import_description,
            R.drawable.ic_file
        ) {
            filePicker.launch(filePickerTypes)
        }
        binding.options.addOption(
            R.string.label_text_import_title,
            R.string.label_text_import_description,
            R.drawable.ic_link
        ) {
            AccountUriDialog(supportFragmentManager, null) { data, dialog ->
                try {
                    showResults(AccountExporter.import(data))
                    dialog.dismiss()
                } catch (e: Exception) {
                    dialog.error = e.message
                }
            }
        }
    }

    private fun showResults(importedData: AccountExporter.ImportedData) {
        showResults(listOf(importedData))
    }

    private fun showResults(importedData: List<AccountExporter.ImportedData>) {
        startActivity(ImportResultActivity::class) {
            putExtra(ImportResultActivity.EXTRA_DATA, ArrayList(importedData)) //FIXME: Start on top?
        }
    }
}