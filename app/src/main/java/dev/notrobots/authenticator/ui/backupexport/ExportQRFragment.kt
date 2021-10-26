package dev.notrobots.authenticator.ui.backupexport

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.printHTML
import dev.notrobots.androidstuff.extensions.printImage
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.QRCode
import dev.notrobots.authenticator.models.QRCodeStyle
import dev.notrobots.authenticator.views.ImageSlider
import kotlinx.android.synthetic.main.fragment_export_qr.*
import kotlinx.android.synthetic.main.view_imageslider.view.*
import org.apache.commons.codec.binary.Base64
import java.io.ByteArrayOutputStream

class ExportQRFragment : Fragment() {
    private var qrCodeStyle = QRCodeStyle.Default    //TODO: The style should be set in the ExportDetailActivity
    private var currentQRCode = 0
    private val styleMap = mapOf(
        R.id.menu_export_style_default to QRCodeStyle.Default,
        R.id.menu_export_style_rainbow to QRCodeStyle.Rainbow,
        R.id.menu_export_style_trans to QRCodeStyle.Trans
    )
    private val saveCurrentQRCode = registerForActivityResult(CreateDocument()) {
        it?.let {
            saveQRCode(qrCodes[currentQRCode], it)
        }
    }
    private val saveAllQRCodes = registerForActivityResult(OpenDocumentTree()) {
        it?.let {
            val baseName = exportFileName
            val document = DocumentFile.fromTreeUri(requireContext(), it)

            if (document != null) {
                for ((index, code) in qrCodes.withIndex()) {
                    val name = baseName.replace(".png", "_$index.png")
                    val uri = document.createFile("image/png", name)!!.uri

                    saveQRCode(code, uri)
                }
            } else {
                requireContext().makeToast("Cannot save files")
            }
        }
    }
    private val exportFileName
        get() = "authenticator_qr_${now() / 100}.png"
    var qrCodes: List<QRCode> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_export_qr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pager_export_output.nextView = btn_export_next
        pager_export_output.previousView = btn_export_previous
        pager_export_output.infiniteScroll = true
        pager_export_output.indicatorView.visibility = View.GONE

        updateImages()

        if (qrCodes.size == 1) {
            btn_export_previous.visibility = View.INVISIBLE
            btn_export_next.text = "Done"
            btn_export_next.setOnClickListener {
                requireActivity().finish()
            }
            text_export_title.text = "QR code"
        } else {
            pager_export_output.setCallback(object : ImageSlider.Callback {
                override fun onNextImage(view: View, position: Int) {}

                override fun onPreviousImage(view: View, position: Int) {}

                override fun onImageChanged(view: ImageSlider, old: Int, new: Int) {
                    if (old == qrCodes.lastIndex && new == 0) { //FIXME: This doesn't really work well
                        view.imageslider_pager.currentItem = old
                        requireActivity().finish()
                    }

                    (view.nextView as Button).text = if (new == qrCodes.lastIndex) "Done" else "Next"
                    view.previousView?.visibility = if (new == 0) View.INVISIBLE else View.VISIBLE
                    text_export_title.text = "QR code ${new + 1} of ${qrCodes.size}"
                    currentQRCode = new
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_export_qr, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export_copy_current -> {
                requireContext().copyToClipboard(qrCodes[currentQRCode])
                requireContext().makeToast("Copied to clipboard")
            }
            R.id.menu_export_copy_all -> {
                requireContext().copyToClipboard(qrCodes.joinToString("\n"))
                requireContext().makeToast("Copied to clipboard")
            }
            R.id.menu_export_save_current -> {
                saveCurrentQRCode.launch(exportFileName)
            }
            R.id.menu_export_save_all -> {
                saveAllQRCodes.launch(null)
            }
            R.id.menu_export_print_current -> {
                val bitmap = qrCodes[currentQRCode].toBitmap(qrCodeStyle)

                requireContext().printImage(bitmap)
            }
            R.id.menu_export_print_all -> {
                //TODO: Show more info and put all QRs in one file if possible
                val html = buildString {
                    append("<html><body>")

                    for (qrCode in qrCodes) {
                        val stream = ByteArrayOutputStream()
                        val bitmap = qrCode.toBitmap(qrCodeStyle)

                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

                        val bytes = stream.toByteArray()
                        val base64 = Base64.encodeBase64String(bytes)
                        val imgSrc = "data:image/png;base64,$base64"

                        append("<img src='$imgSrc' /><br/>")
                    }

                    append("</body></html>")
                }

                requireContext().printHTML(html)    //TODO: Specify the job
            }

            else -> {
                if (item.itemId in styleMap) {
                    qrCodeStyle = styleMap[item.itemId]!!
                    item.isChecked = true
                    updateImages()
                }
            }
        }

        return true
    }

    /**
     * Saves the given [qrCode] to the given [location]
     */
    private fun saveQRCode(qrCode: QRCode, location: Uri) {
        val stream = requireContext().contentResolver.openOutputStream(location)
        val bitmap = qrCode.toBitmap(qrCodeStyle)

        //TODO: The QR img should have additional text info

        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            requireContext().makeToast("Saved successfully")
        } catch (e: Exception) {
            requireContext().makeToast("Cannot save file")
        } finally {
            stream?.close()
        }
    }

    /**
     * Updates the images with the new [QRCodeStyle]
     */
    private fun updateImages() {
        pager_export_output.setImageBitmaps(qrCodes.map { it.toBitmap(qrCodeStyle) })
    }
}