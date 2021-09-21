package dev.notrobots.authenticator.google

interface Clock {
    fun nowMillis(): Long
}
