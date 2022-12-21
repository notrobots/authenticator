package dev.notrobots.authenticator.extensions

inline fun <reified E : Enum<E>> Enum.Companion.first(): E {
    return E::class.java.enumConstants.first()
}