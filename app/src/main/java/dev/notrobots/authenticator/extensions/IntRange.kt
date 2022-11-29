package dev.notrobots.authenticator.extensions

internal fun IntRange.dropLast(): IntRange {
    return start until endInclusive
}

internal fun IntRange.dropFirst(): IntRange {
    return (start - 1)..endInclusive
}