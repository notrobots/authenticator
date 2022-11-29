package dev.notrobots.authenticator.extensions

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.LayoutRes

internal fun LayoutInflater.inflate(@LayoutRes layout: Int): View {
    return inflate(layout, LinearLayout(context), false)
}