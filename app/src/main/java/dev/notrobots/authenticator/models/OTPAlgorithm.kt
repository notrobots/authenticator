package dev.notrobots.authenticator.models

enum class OTPAlgorithm(val algorithmName: String, val bytes: Int) {
    SHA1("HmacSHA1", 20),
    SHA256("HmacSHA256", 32),
    SHA512("HmacSHA512", 64)
}