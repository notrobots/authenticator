package dev.notrobots.authenticator.util

import java.util.*

inline fun <reified E : Enum<E>> parseEnum(value: String?, ignoreCase: Boolean = false): E? {
    val values = E::class.java.enumConstants

    return values.find { it.name.equals(value, ignoreCase) }
}

fun requireNotNull(vararg values: Any?, lazyMessage: () -> String) {
    if (values.any { it == null }) {
        throw Exception(lazyMessage())
    }
}

fun requireNotEmpty(vararg values: String?, lazyMessage: () -> String) {
    if (values.any { it == null || it.isEmpty() }) {
        throw Exception(lazyMessage())
    }
}

fun now(): Long = System.currentTimeMillis()
