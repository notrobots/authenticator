package dev.notrobots.authenticator.extensions

import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setClearErrorOnType() {
    editText?.addTextChangedListener {
        error = null
    }
}