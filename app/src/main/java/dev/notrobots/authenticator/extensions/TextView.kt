package dev.notrobots.authenticator.extensions

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat

fun TextView.setHTMLText(@StringRes res: Int, vararg args: Any) {
    val text = context.resources.getText(res, args).toString()

    setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY))
}

fun TextView.setHTMLText(text: CharSequence, vararg args: Any) {
    val text = String.format(text.toString(), args)

    setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY))
}