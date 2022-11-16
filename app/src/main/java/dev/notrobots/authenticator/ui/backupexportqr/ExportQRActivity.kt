package dev.notrobots.authenticator.ui.backupexportqr

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityExportQrBinding
import dev.notrobots.authenticator.models.QRCode
import dev.notrobots.authenticator.models.QRCodePaint
import dev.notrobots.authenticator.models.AuthenticatorQRCodePaint
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.views.ImageSlider
import org.apache.commons.codec.binary.Base64
import java.io.ByteArrayOutputStream

class ExportQRActivity : AuthenticatorActivity() {
    private val binding by viewBindings<ActivityExportQrBinding>()
    private val saveAll = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        it?.let {
            val document = DocumentFile.fromTreeUri(this, it)

            if (document != null) {
                for ((index, code) in qrCodes.withIndex()) {
                    val name = getFilename().replace(".png", "_$index.png")
                    val uri = document.createFile("image/png", name)!!.uri

                    saveQRCode(code, uri)
                }
            } else {
                makeToast("Cannot save files")
            }
        }
    }
    private var qrCodes: List<QRCode> = emptyList()
    private lateinit var qrCodePaint: QRCodePaint
    private var qrCodeType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        finishOnBackPressEnabled = true

        qrCodePaint = AuthenticatorQRCodePaint(this)
        qrCodes = intent.getSerializableExtra(EXTRA_QR_CODES) as List<QRCode>
        qrCodeType = intent.getIntExtra(EXTRA_QR_TYPE, QR_TYPE_DEFAULT)

        binding.imageSlider.indicatorView?.disable()
        binding.imageSlider.setImageBitmaps(qrCodes.map { it.toBitmap(qrCodePaint) })
        binding.done.setOnClickListener {
            finish()    //FIXME: Back to MainActivity
        }

        when (qrCodeType) {
            QR_TYPE_DEFAULT -> {
                binding.description.text = "Open the Authenticator app on your other device and scan these QR codes or store them in a safe location"
            }

            QR_TYPE_GOOGLE_AUTHENTICATOR -> {
                binding.description.text = "Open the Google Authenticator app on your other device and scan these QR codes or store them in a safe location"
            }
        }

        if (qrCodes.size == 1) {
            binding.imageSlider.nextView?.disable()
            binding.imageSlider.previousView?.disable()
            binding.toolbarLayout.title = "QR code"
        } else {
            binding.imageSlider.nextView?.show()
            binding.imageSlider.previousView?.show()
            binding.imageSlider.setCallback(object : ImageSlider.Callback {
                override fun onNextImage(view: View, position: Int) {
                    //FIXME: Hide the next button when the index is size - 1
                }

                override fun onPreviousImage(view: View, position: Int) {
                    //FIXME: Hide the previous button when the index is 0
                }

                override fun onImageChanged(view: ImageSlider, old: Int, new: Int) {
                    binding.toolbarLayout.title = "QR code ${new + 1} of ${qrCodes.size}"
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_export_qr, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export_copy -> {
                copyToClipboard(qrCodes.joinToString("\n"))
                makeToast("Copied to clipboard")
            }
            R.id.menu_export_save -> {
                saveAll.launch(null)
            }
            R.id.menu_export_print -> {
                printBackup()
            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    /**
     * Saves the given [qrCode] to the given [location].
     */
    private fun saveQRCode(qrCode: QRCode, location: Uri) {
        val stream = contentResolver.openOutputStream(location)
        val bitmap = qrCode.toBitmap()

        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            makeToast("Saved successfully")
        } catch (e: Exception) {
            makeToast("Cannot save file")
        } finally {
            stream?.close()
        }
    }

    /**
     * Formats and prints the backup data.
     */
    private fun printBackup() {
        val template = resources.openRawResource(R.raw.qr_backup_template)
            .bufferedReader()
            .readText()
        val data = qrCodes.joinToString(",") {
            val bitmap = it.toBitmap()
            val stream = ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
            }
            val bytes = stream.toByteArray()
            val base64 = Base64.encodeBase64String(bytes)

            "'data:image/png;base64,$base64'"
        }
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        val backupTitle = when (qrCodeType) {
            QR_TYPE_DEFAULT -> "Authenticator Backup"
            QR_TYPE_GOOGLE_AUTHENTICATOR -> "Google Authenticator Backup"

            else -> throw Exception("Unknown QR code type")
        }

        printHTML(
            template
                .replace("\$QR_DATA_ARRAY", data)
                .replace("\$BACKUP_TITLE", backupTitle),
            "Authenticator Backup print",
            printAttributes,
            true
        )
    }

    /**
     * Generates a new file name.
     */
    private fun getFilename(): String {
        return when (qrCodeType) {
            QR_TYPE_DEFAULT -> BackupManager.qrBackupFilename
            QR_TYPE_GOOGLE_AUTHENTICATOR -> BackupManager.googleAuthenticatorBackupFilename

            else -> throw Exception("Unknown QR code type")
        }
    }

    companion object {
        const val EXTRA_QR_CODES = "ExportQRActivity.QR_CODES"
        const val EXTRA_QR_TYPE = "ExportQRActivity.QR_TYPE"
        const val QR_TYPE_DEFAULT = 0
        const val QR_TYPE_GOOGLE_AUTHENTICATOR = 100
    }
}