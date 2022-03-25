package dev.notrobots.authenticator.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

object ViewUtil {
    fun inflate(
        @LayoutRes layout: Int,
        root: ViewGroup,
        attachToParent: Boolean = false
    ): View {
        return LayoutInflater.from(root.context).inflate(
            layout,
            root,
            attachToParent
        )
    }

    fun hitTest(v: View, x: Int, y: Int): Boolean {
        val tx = (v.translationX + 0.5f).toInt()
        val ty = (v.translationY + 0.5f).toInt()
        val left = v.left + tx
        val right = v.right + tx
        val top = v.top + ty
        val bottom = v.bottom + ty

        return x >= left && x <= right && y >= top && y <= bottom
    }
}