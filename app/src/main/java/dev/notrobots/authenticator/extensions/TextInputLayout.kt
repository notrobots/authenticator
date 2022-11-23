package dev.notrobots.authenticator.extensions

import android.text.InputFilter
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setMaxLength(
    maxLength: Int,
    enableCounter: Boolean = true
) {
    editText?.let {
        it.filters += InputFilter.LengthFilter(maxLength)
    }

    counterMaxLength = maxLength
    isCounterEnabled = enableCounter
}