package dev.notrobots.authenticator.ui.backupexportqr

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityExportQrBinding
import dev.notrobots.authenticator.models.QRCode
import dev.notrobots.authenticator.models.QRCodeStyle
import dev.notrobots.authenticator.views.ImageSlider
import org.apache.commons.codec.binary.Base64
import java.io.ByteArrayOutputStream

class ExportQRActivity : AppCompatActivity() {
    private val binding by viewBindings<ActivityExportQrBinding>()
    private var currentQRCode = 0
    private val saveCurrentQRCode = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        it?.let {
            saveQRCode(qrCodes[currentQRCode], it)
        }
    }
    private val saveAllQRCodes = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
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
    private var qrStyle = QRCodeStyle.Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val uris = intent.getSerializableExtra(EXTRA_QR_CODES) as List<Uri>

        qrStyle = intent.getSerializableExtra(EXTRA_QR_STYLE) as QRCodeStyle
        qrCodes = uris.map { QRCode(it) }

        //binding.imageSlider.infiniteScroll = true   //FIXME: If this is enabled, the next and previous should never have the "disabled" color
        binding.imageSlider.indicatorView.visibility = View.GONE
        binding.imageSlider.setImageBitmaps(qrCodes.map { it.toBitmap(qrStyle) })
        binding.done.setOnClickListener {
            finish()    //FIXME: Back to MainActivity
        }

        if (qrCodes.size == 1) {
            binding.title.text = "QR code"
        } else {
            binding.imageSlider.setCallback(object : ImageSlider.Callback {
                override fun onNextImage(view: View, position: Int) {}

                override fun onPreviousImage(view: View, position: Int) {}

                override fun onImageChanged(view: ImageSlider, old: Int, new: Int) {
                    binding.title.text = "QR code ${new + 1} of ${qrCodes.size}"
                    currentQRCode = new
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
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.menu_export_copy_current -> {
                copyToClipboard(qrCodes[currentQRCode])
                makeToast("Copied to clipboard")
            }
            R.id.menu_export_copy_all -> {
                copyToClipboard(qrCodes.joinToString("\n"))
                makeToast("Copied to clipboard")
            }
            R.id.menu_export_save_current -> {
                saveCurrentQRCode.launch(getFilename())
            }
            R.id.menu_export_save_all -> {
                saveAllQRCodes.launch(null)
            }
            R.id.menu_export_print_current -> {
                val bitmap = qrCodes[currentQRCode].toBitmap(qrStyle)

                printImage(bitmap)
            }
            R.id.menu_export_print_all -> {
                //TODO: Show more info and put all QRs in one file if possible
                val html = buildString {
                    append("<html><body>")

                    for (qrCode in qrCodes) {
                        val stream = ByteArrayOutputStream()
                        val bitmap = qrCode.toBitmap(qrStyle)

                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

                        val bytes = stream.toByteArray()
                        val base64 = Base64.encodeBase64String(bytes)
                        val imgSrc = "data:image/png;base64,$base64"

                        append("<img src='$imgSrc' /><br/>")
                    }

                    append("</body></html>")
                }

                printHTML(html)    //TODO: Specify the job
            }
        }

        return true
    }

    /**
     * Saves the given [qrCode] to the given [location]
     */
    private fun saveQRCode(qrCode: QRCode, location: Uri) {
        val stream = contentResolver.openOutputStream(location)
        val bitmap = qrCode.toBitmap(qrStyle)

        //TODO: The QR img should have additional text info

        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            makeToast("Saved successfully")
        } catch (e: Exception) {
            makeToast("Cannot save file")
        } finally {
            stream?.close()
        }
    }

    private fun getFilename(): String {
        return "authenticator_qr_${now() / 100}.png"
    }

    companion object {
        const val EXTRA_QR_CODES = "ExportQRActivity.QR_CODES"
        const val EXTRA_QR_STYLE = "ExportQRActivity.QR_STYLE"
    }
}