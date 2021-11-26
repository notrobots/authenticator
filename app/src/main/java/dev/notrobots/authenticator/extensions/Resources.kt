package dev.notrobots.authenticator.extensions

import android.content.res.Resources
import android.text.Html
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

fun Resources.getText(@StringRes id: Int, vararg args: Any): CharSequence {
    val raw = getString(id)
    val text = String.format(raw, *args)

    return Html.fromHtml(text)
}

fun Resources.getQuantityText(@PluralsRes id: Int, quantity: Int, vararg args: Any): CharSequence {
    val raw = getQuantityString(id, quantity)
    val text = String.format(raw, *args)

    return Html.fromHtml(text)
}