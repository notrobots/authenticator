package dev.notrobots.authenticator.models

import android.content.Context
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.authenticator.R

class AuthenticatorQRCodePaint(context: Context) : QRCodePaint() {
    private val background = context.resolveColorAttribute(android.R.attr.windowBackground)
    private val foreground = context.resolveColorAttribute(R.attr.colorPrimary)

    override fun getPixel(x: Int, y: Int, state: Boolean): Int {
        return if (state) foreground else background
    }
}