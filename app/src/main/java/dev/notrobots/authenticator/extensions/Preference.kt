package dev.notrobots.authenticator.extensions

import androidx.preference.Preference

fun <T : Preference> T.setTypedSummaryProvider(provider: (T) -> Any?) {
    summaryProvider = Preference.SummaryProvider<T> {
        provider(it).toString()
    }
}

/**
 * Updates the summary using the value returned by the `summaryProvider`.
 *
 * If no summaryProvider was defined the summary won't be changed.
 */
fun <T : Preference> T.updateSummary() {
    val provider = summaryProvider  //FIXME: Not working. This needs to copy the provider

    summaryProvider = null
    provider?.let {
        summary = it.provideSummary(this)
    }
    summaryProvider = provider
}