package dev.notrobots.authenticator.ui.backupimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts.*
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.makeSnackBar
import dev.notrobots.androidstuff.extensions.showInfo
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityImportBinding
import dev.notrobots.authenticator.dialogs.AddAccountUriDialog
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.ui.backupimportresult.ImportResultActivity
import dev.notrobots.authenticator.util.AccountExporter

@AndroidEntryPoint
class ImportActivity : ThemedActivity() {
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
                val uri = it.data!!.getStringExtra(BarcodeScannerActivity.EXTRA_QR_DATA) ?: ""

                try {
                    val data = AccountExporter.import(uri)

                    showResults(data)
                } catch (e: Exception) {
                    showInfo("Error", e.message)
                }
            }
        }
    }
    private val filePicker = registerForActivityResult(OpenDocument()) {
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
                        showInfo("Error", "Import data is corrupt")
                    }
                }
            }

            else -> makeSnackBar("Invalid file type", binding.root)
        }
    }
    private val binding by viewBindings<ActivityImportBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener { finish() }

        binding.options.addOption(
            "QR Code",
            "Scan one of multiple QR codes",
            R.drawable.ic_qr
        ) {
            val intent = Intent(this, BarcodeScannerActivity::class.java)

            barcodeScanner.launch(intent)
        }
        binding.options.addOption(
            "File",
            "Load a text or image file",
            R.drawable.ic_qr
        ) {
            val types = arrayOf(
                "image/*",  //TODO: Specify the image type
                "text/*"
            )

            filePicker.launch(types)
        }
        binding.options.addOption(
            "Text",
            "Input a string",
            R.drawable.ic_qr
        ) {
            AddAccountUriDialog(supportFragmentManager) { data, dialog ->
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
        startActivity(ImportResultActivity::class) {
            putExtra(ImportResultActivity.EXTRA_DATA, importedData) //FIXME: Start on top?
        }
    }
}