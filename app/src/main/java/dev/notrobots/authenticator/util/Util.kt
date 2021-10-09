package dev.notrobots.authenticator.util

import android.content.Context
import android.widget.ArrayAdapter
import com.google.protobuf.ByteString
import org.apache.commons.codec.binary.Base32
import kotlin.reflect.KClass

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

fun byteString(string: String): ByteString {
    return ByteString.copyFrom(string.toByteArray())
}
