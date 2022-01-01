package dev.notrobots.authenticator.models

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.zxing.common.BitMatrix
import java.io.Serializable

abstract class QRCodeStyle : Serializable {
    abstract fun paint(width: Int, height: Int, bitmap: Bitmap, matrix: BitMatrix)

    companion object {
        val Default = object : QRCodeStyle() {
            override fun paint(width: Int, height: Int, bitmap: Bitmap, matrix: BitMatrix) {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }
        }
        val Inverted = object : QRCodeStyle() {
            override fun paint(width: Int, height: Int, bitmap: Bitmap, matrix: BitMatrix) {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) Color.WHITE else Color.BLACK)
                    }
                }
            }
        }
        val Rainbow = object : QRCodeStyle() {
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
        val Trans = object : QRCodeStyle() {
            private val colors = mutableListOf(
                Color.parseColor("#5BCEFA"),
                Color.parseColor("#F5A9B8"),
                Color.parseColor("#FFFFFF"),    //FIXME: White isn't visible sadly
                Color.parseColor("#F5A9B8"),
                Color.parseColor("#5BCEFA")
            )

            override fun paint(width: Int, height: Int, bitmap: Bitmap, matrix: BitMatrix) {
                val columnWidth = width / colors.size
                var columnCursor = 0
                var currentColor = 0

                for (y in 0 until height) {
                    columnCursor = 0
                    currentColor = 0

                    for (x in 0 until width) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) colors[currentColor] else Color.parseColor("#262626"))

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