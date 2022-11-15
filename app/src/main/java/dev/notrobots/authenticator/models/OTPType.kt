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

    //TODO: Steam, mOTP

    companion object {
        fun stringValues(): List<String> {
            return values().map {
                it.toString().lowercase()
            }
        }

        fun contains(value: String?): Boolean {
            return values().find { it.name.equals(value, true) } != null
        }
    }
}