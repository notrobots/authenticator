package dev.notrobots.authenticator.util

import org.apache.commons.codec.binary.Base32

inline fun <reified E : Enum<E>> parseEnum(value: String?, ignoreCase: Boolean = false): E? {
    val values = E::class.java.enumConstants

    return values.find { it.name.equals(value, ignoreCase) }
}

fun now(): Long = System.currentTimeMillis()

fun error(message: String): Nothing {
    throw Exception(message)
}

fun isValidBase32(base32: String): Boolean {
    return Base32().isInAlphabet(base32) && base32.length % 8 == 0
}