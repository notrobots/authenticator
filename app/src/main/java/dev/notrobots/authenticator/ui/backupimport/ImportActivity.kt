package dev.notrobots.authenticator.ui.backupimport

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.androidstuff.util.Logger.Companion.logd
import dev.notrobots.androidstuff.util.Logger.Companion.loge
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityImportBinding
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.dialogs.AccountUriDialog
import dev.notrobots.authenticator.dialogs.ReplaceAccountDialog
import dev.notrobots.authenticator.models.ImportData
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.ui.backupimportresult.ImportResultActivity
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.util.BackupUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                val uris = it.data!!.getStringArrayListExtra(BarcodeScannerActivity.EXTRA_QR_LIST) ?: emptyList<String>()

                try {
                    val backupData = BackupManager.importList(uris)

//                    if (data.isEmpty) {       XXX: Was this needed?
//                        showInfo("Error", "Invalid data")
//                    }


                    lifecycleScope.launch {
                        BackupUtil.importBackupData(this@ImportActivity, backupData, accountDao)
                    }
                } catch (e: Exception) {
                    loge("QR content: $uris")
                    loge("There was an error while importing a QR code", e)
                    showInfo("Error", "Invalid data: $e")
                }
            }
        }
    }
    private val filePicker = registerForActivityResult(OpenDocument()) {
        // If the user goes back while picking a file the uri will be null
        if (it != null) {
            val mimeType = contentResolver.getType(it) ?: ""

            when (mimeType) {
                "image/jpeg", "image/png" -> {
                    val image = InputImage.fromFilePath(this, it)

                    scanner.process(image)
                        .addOnSuccessListener {
                            val content = it.first().rawValue

                            try {
                                val backupData = BackupManager.importText(content ?: "")

                                lifecycleScope.launch {
                                    BackupUtil.importBackupData(this@ImportActivity, backupData, accountDao)
                                }
                            } catch (e: Exception) {
                                loge("File content: $content")
                                loge("There was an error while importing a backup file", e)
                                showInfo("Error", "Import data is corrupt")//FIXME: Show detailed error with account index ecc
                            }
                        }
                        .addOnFailureListener {
                            makeSnackBar("Error scanning QR", binding.root)
                        }
                }
                "text/plain", "application/json" -> {
                    contentResolver.openInputStream(it)?.let {
                        val content = it.reader().readText()

                        try {
                            val backupData = BackupManager.importText(content)

                            lifecycleScope.launch {
                                BackupUtil.importBackupData(this@ImportActivity, backupData, accountDao)
                            }
                        } catch (e: Exception) {
                            loge("File content: $content")
                            loge("There was an error while importing a backup file", e)
                            showInfo("Error", "Import data is corrupt")
                        }
                    }
                }
                "application/octet-stream" -> {
                    //TODO Encrypted backup
                }

                else -> makeSnackBar("Invalid file type", binding.root)
            }
        }
    }
    private val filePickerTypes = arrayOf(
        "image/jpeg",               // .jpeg/.jpg
        "image/png",                // .png
        "text/plain",               // .txt
        "application/json",         // .json
        "application/octet-stream"  // .bin
    )
    private val binding by viewBindings<ActivityImportBinding>()

    @Inject
    protected lateinit var accountDao: AccountDao

    @Inject
    protected lateinit var tagDao: TagDao

    @Inject
    protected lateinit var accountTagCrossRefDao: AccountTagCrossRefDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        finishOnBackPressEnabled = true

        binding.importOptionQr.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java).apply {
                putExtra(BarcodeScannerActivity.EXTRA_MULTI_SCAN, true)
            }

            barcodeScanner.launch(intent)
        }
        binding.importOptionFile.setOnClickListener {
            filePicker.launch(filePickerTypes)
        }
        binding.importOptionText.setOnClickListener {
            AccountUriDialog(R.string.label_import_option_text)
                .show(supportFragmentManager, null)
        }
    }
}