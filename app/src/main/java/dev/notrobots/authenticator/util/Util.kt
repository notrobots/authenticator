package dev.notrobots.authenticator.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import dev.notrobots.androidstuff.util.bindView
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
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

inline fun <T, reified B : ViewBinding> adapterOf(
    context: Context,
    iterable: Iterable<T>,
    noinline rowFactory: (item: T, position: Int, binding: B) -> Unit
): ArrayAdapter<T> {
    return adapterOf(context, iterable, B::class, rowFactory)
}

fun <T, B : ViewBinding> adapterOf(
    context: Context,
    iterable: Iterable<T>,
    bindingType: KClass<B>,
    rowFactory: (item: T, position: Int, binding: B) -> Unit
): ArrayAdapter<T> {
    return object : ArrayAdapter<T>(context, 0) {
        override fun getCount(): Int {
            return iterable.count()
        }

        override fun getItem(position: Int): T {
            return iterable.elementAt(position)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val binding = if (convertView != null) {
                bindView(bindingType, convertView)
            } else {
                bindView(bindingType, parent)
            }

            return binding.root.also {
                rowFactory(getItem(position), position, binding)
            }
        }
    }
}

fun <T> adapterOf(
    context: Context,
    iterable: Iterable<T>,
    @LayoutRes layout: Int,
    rowFactory: (item: T, position: Int, view: View) -> Unit
): ArrayAdapter<T> {
    val inflater by lazy {
        LayoutInflater.from(context)
    }

    return object : ArrayAdapter<T>(context, 0) {
        override fun getCount(): Int {
            return iterable.count()
        }

        override fun getItem(position: Int): T {
            return iterable.elementAt(position)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(layout, parent, false)

            rowFactory(getItem(position), position, view)

            return view
        }
    }
}

fun <T> adapterOf(
    context: Context,
    iterable: Iterable<T>,
    viewFactory: (item: T, parent: ViewGroup, inflater: LayoutInflater) -> View,
    rowFactory: (item: T, position: Int, view: View) -> Unit
): ArrayAdapter<T> {
    val inflater by lazy {
        LayoutInflater.from(context)
    }

    return object : ArrayAdapter<T>(context, 0) {
        override fun getCount(): Int {
            return iterable.count()
        }

        override fun getItem(position: Int): T {
            return iterable.elementAt(position)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val view = convertView ?: viewFactory(item, parent, inflater)

            rowFactory(getItem(position), position, view)

            return view
        }
    }
}

fun daysToMillis(days: Int): Long {
    return TimeUnit.DAYS.toMillis(days.toLong())
}

fun snakeToPascalCase(name: String): String {
    var result = name.lowercase()
    val underscoreRegex = Regex("[_\\s]")

    for (word in underscoreRegex.findAll(result)) {
        val index = word.range.first + 1
        val range = index..index
        val char = result[index]

        result = result.replaceRange(range, char.uppercase())
    }

    return result.replace(underscoreRegex, "")
}