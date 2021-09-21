package dev.notrobots.authenticator.extensions

infix fun Number.percentOf(other: Number): Double {
    return percentOf(other, 100)
}

fun Number.percentOf(other: Number, max: Number): Double {
    return toDouble() * other.toDouble() / max.toDouble()
}

infix fun Number.isPercentOf(other: Number): Double {
    return isPercentOf(other, 100)
}

fun Number.isPercentOf(other: Number, max: Number): Double {
    return toDouble() / other.toDouble() * max.toDouble()
}