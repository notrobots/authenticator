package dev.notrobots.authenticator.util

import android.content.Context
import androidx.annotation.StringRes

class AuthenticatorException(
    message: String?,
    @StringRes val localizedMessageId: Int
) : Exception(message) {
    constructor(@StringRes localizedMessageId: Int) : this(null, localizedMessageId)

    fun getLocalizedMessage(context: Context): String {
        return context.getString(localizedMessageId)
    }
}