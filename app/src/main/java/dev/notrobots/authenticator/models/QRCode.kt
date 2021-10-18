package dev.notrobots.authenticator.models

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

class QRCode(
    val content: String,
    size: Int
) {
    private val writer = QRCodeWriter()
    private val bitMatrix = writer.encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size
    )
    private val width = bitMatrix.width
    private val height = bitMatrix.height

    override fun toString(): String {
        return content
    }

    fun toMatrix(): BitMatrix {
        return bitMatrix
    }

    fun toBitmap(style: QRCodeStyle): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            style.paint(width, height, this, bitMatrix)
        }
    }

    fun toBitmap(): Bitmap {
        return toBitmap(QRCodeStyle.Default)
    }
}