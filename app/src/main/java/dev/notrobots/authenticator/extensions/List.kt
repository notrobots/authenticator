package dev.notrobots.authenticator.extensions

fun <T> List<T>.subList(vararg indexes: Int): List<T> {
    return indexes.map { this[it] }
}

fun <T> List<T>.subList(indexes: List<Int>): List<T> {
    return indexes.map { this[it] }
}