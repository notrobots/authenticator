package dev.notrobots.authenticator.models

import java.util.*

enum class OTPType {
    /**
     * Time based One Time Password
     */
    TOTP,

    /**
     * HMAC based One Time Password
     */
    HOTP;

    companion object {
        fun stringValues(): List<String> {
            return values().map {
                it.toString().toLowerCase(Locale.getDefault())
            }
        }
    }
}