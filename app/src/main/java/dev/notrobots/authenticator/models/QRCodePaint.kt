package dev.notrobots.authenticator.models

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.common.BitMatrix
import java.io.Serializable

abstract class QRCodePaint : Serializable {
    abstract fun paint(width: Int, height: Int, bitmap: Bitmap, matrix: BitMatrix)

    companion object {
        val Default = object : QRCodePaint() {
            override fun paint(width: Int, height: Int, bitmap: Bitmap, matrix: BitMatrix) {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }
        }
        val Rainbow = object : QRCodePaint() {
            private val colors = mutableListOf(
                Color.parseColor("#D12229"),
                Color.parseColor("#F68A1E"),
                Color.parseColor("#FDE01A"),
                Color.parseColor("#007940"),
                Color.parseColor("#24408E"),
                Color.parseColor("#732982")
            )

            override fun paint(width: Int, height: Int, bitmap: Bitmap, matrix: BitMatrix) {
                val columnWidth = width / colors.size
                var columnCursor = 0
                var currentColor = 0

                for (y in 0 until height) {
                    columnCursor = 0
                    currentColor = 0

                    for (x in 0 until width) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) colors[currentColor] else Color.WHITE)

                        columnCursor++

                        if (columnCursor == columnWidth) {
                            columnCursor = 0
                            currentColor++

                            if (currentColor == colors.size) {
                                currentColor = 0
                            }
                        }
                    }
                }

//                for (x in 0 until width) {
//                    for (y in 0 until height) {
//                        bitmap.setPixel(x, y, colors[columnCursor])
//
//
//                        bitmap.setPixel(x, y, if (matrix[x, y]) Color.WHITE else Color.BLACK)
//                    }
//                    column
//                }
            }
        }
    }
}