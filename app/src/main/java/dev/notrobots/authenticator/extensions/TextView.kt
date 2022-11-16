package dev.notrobots.authenticator.extensions

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat

fun TextView.setHTMLText(@StringRes res: Int, vararg args: Any) {
    val text = context.resources.getText(res, args).toString()

    setHTMLText(text)
}

fun TextView.setHTMLText(@StringRes res: Int) {
    val text = context.resources.getText(res).toString()

    setHTMLText(text)
}

fun TextView.setHTMLText(text: CharSequence, vararg args: Any) {
    val text = String.format(text.toString(), args)

    setHTMLText(text)
}

fun TextView.setHTMLText(text: CharSequence) {
    setText(HtmlCompat.fromHtml(text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY))
}