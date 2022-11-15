package dev.notrobots.authenticator.extensions

fun <K, V> Map<K, V>.find(predicate: (K, V) -> Boolean): Map.Entry<K, V>? {
    for (entry in entries) {
        if (predicate(entry.key, entry.value)) {
            return entry
        }
    }

    return null
}

fun <T> Map<T, *>.slice(keys: Iterable<T>) = filterKeys { it in keys }