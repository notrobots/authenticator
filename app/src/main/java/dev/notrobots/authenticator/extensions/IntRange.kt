package dev.notrobots.authenticator.extensions

fun IntRange.dropLast(): IntRange {
    return start until endInclusive
}

fun IntRange.dropFirst(): IntRange {
    return (start - 1)..endInclusive
}