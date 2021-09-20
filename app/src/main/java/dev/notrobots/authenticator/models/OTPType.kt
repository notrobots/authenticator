package dev.notrobots.authenticator.models

enum class OTPType {
    /**
     * Time based One Time Password
     */
    TOTP,

    /**
     * HMAC based One Time Password
     */
    HOTP
}