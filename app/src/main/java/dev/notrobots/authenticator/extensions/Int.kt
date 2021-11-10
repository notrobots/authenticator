package dev.notrobots.authenticator.extensions

infix fun Int.absoluteRangeTo(that: Int): IntRange {
    return if (this <= that) this..that else that..this
}

fun Int.toHex(): String {
    return "%x".format(this)
}

fun Int.to0x(): String {
    return "0x" + toHex()
}