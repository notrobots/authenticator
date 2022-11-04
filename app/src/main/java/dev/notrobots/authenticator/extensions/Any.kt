package dev.notrobots.authenticator.extensions

fun <T> T.takeIf(default: T, predicate: (T) -> Boolean): T {
    return if (predicate(this)) this else default
}

operator fun <T> T.invoke(editor: T.() -> Unit): T {
    return apply(editor)
}