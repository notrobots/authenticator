package dev.notrobots.authenticator.extensions

fun <T> Iterable<T>.contentEquals(other: Iterable<T>, comparator: (a: T, b: T) -> Boolean): Boolean {
    if (count() == 0 && other.count() == 0) {
        return true
    }

    if (count() != other.count()) {
        return false
    }

    return allIndexed { i, el ->
        comparator(el, other.elementAt(i))
    }
}

fun <T> Iterable<T>.allIndexed(predicate: (Int, T) -> Boolean): Boolean {
    if (count() == 0) {
        return true
    }

    for ((index, element) in this.withIndex()) {
        if (!predicate(index, element)) {
            return false
        }
    }

    return true
}
