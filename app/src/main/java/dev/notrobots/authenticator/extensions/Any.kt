package dev.notrobots.authenticator.extensions

internal fun <T> T.takeIf(default: T, predicate: (T) -> Boolean): T {
    return if (predicate(this)) this else default
}