package dev.notrobots.authenticator.util

import android.content.Context
import android.widget.ArrayAdapter
import org.apache.commons.codec.binary.Base32
import kotlin.reflect.jvm.isAccessible

fun isValidBase32(base32: String): Boolean {
    return Base32().isInAlphabet(base32) && base32.length % 8 == 0
}

fun <T> adapterOf(context: Context, iterable: Iterable<T>): ArrayAdapter<T> {
    return ArrayAdapter(
        context,
        android.R.layout.simple_list_item_1,
        iterable.toList()
    )
}

inline fun <reified T> lazyType(crossinline initializer: T.() -> Unit = {}): Lazy<T> {
    val type = T::class
    val emptyConstructor = type.constructors.find {
        it.parameters.isEmpty()
    } ?: throw Exception("Type $type has no empty constructor")

    emptyConstructor.isAccessible = true

    return lazy {
        emptyConstructor.call().apply(initializer)
    }
}

