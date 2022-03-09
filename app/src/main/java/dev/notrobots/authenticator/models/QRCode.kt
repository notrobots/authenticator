package dev.notrobots.authenticator.models

import android.graphics.Bitmap
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.io.Serializable

class QRCode(
    val content: String,
    val size: Int = DEFAULT_SIZE
) : Serializable {
    constructor(uri: Uri, size: Int = DEFAULT_SIZE) : this(uri.toString(), size)

    override fun toString(): String {
        return content
    }

    /**
     * Returns the [BitMatrix] containing the QR data.
     */
    fun toMatrix(): BitMatrix {
        val writer = QRCodeWriter()

        return writer.encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
    }

    /**
     * Returns the [Bitmap] representing the QR data,
     * using the given [paint] to paint the pixels.
     */
    fun toBitmap(paint: QRCodePaint): Bitmap {
        val bitMatrix = toMatrix()
        val width = bitMatrix.width
        val height = bitMatrix.height

        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            paint.paint(width, height, this, bitMatrix)
        }
    }

    /**
     * Returns the [Bitmap] representing the QR data.
     */
    fun toBitmap(): Bitmap {
        return toBitmap(QRCodePaint.Default)
    }

    companion object {
        const val DEFAULT_SIZE = 512
    }
}