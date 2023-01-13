package dev.notrobots.authenticator.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewbinding.ViewBinding
import com.google.gson.Gson
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.androidstuff.util.requireNotEmpty
import org.apache.commons.codec.binary.Base32
import java.util.Arrays
import java.util.concurrent.TimeUnit
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

/**
 * Returns the hashCode of the given fields.
 *
 * This is meant to be used inside your class' hashCode() method
 */
fun hashCodeOf(vararg fields: Any?): Int {
    requireNotEmpty(fields) {
        "You must provide at least one field"
    }

    // Start with a non-zero constant. Prime is preferred.
    var result = 17

    for (field in fields) {
        result *= 31

        when (field) {
            is Boolean -> result += (if (field) 1 else 0)               // 1 bit   » 32-bit
            is Byte -> result += field                                  // 8 bits  » 32-bit
            is Char -> result += field.code                             // 16 bits  » 32-bit
            is Short -> result += field                                 // 16 bits  » 32-bit
            is Int -> result += field                                   // 32 bits  » 32-bit
            is Long -> result += (field xor (field ushr 32)).toInt()    // 64 bits  » 32-bit
            is Float -> result += field.toBits()                        // 32 bits  » 32-bit
            is Double -> {
                val bits = field.toBits()
                result += (bits xor (bits ushr 32)).toInt()             // 64 bits (double) » 64-bit (long) » 32-bit (int)
            }
            is Array<*> -> Arrays.hashCode(field)
            is ByteArray -> Arrays.hashCode(field)
            is CharArray -> Arrays.hashCode(field)
            is ShortArray -> Arrays.hashCode(field)
            is IntArray -> Arrays.hashCode(field)
            is LongArray -> Arrays.hashCode(field)
            is FloatArray -> Arrays.hashCode(field)
            is DoubleArray -> Arrays.hashCode(field)

            else -> field.hashCode()                                    // var bits » 32-bit
        }
    }

    return result;
}

fun <T> Set(size: Int, init: (index: Int) -> T): Set<T> {
    val set = HashSet<T>(size)
    repeat(size) { set.add(init(it)) }
    return set
}

inline fun <reified T> cloneObject(instance: T): T {
    val gson = Gson()

    return gson.fromJson(gson.toJson(instance), T::class.java)
}

fun <T : ViewBinding> bindCustomView(type: KClass<T>, parent: ViewGroup) = bindView(type, parent, true)

inline fun <reified T : ViewBinding> bindCustomView(parent: ViewGroup) = bindView<T>(parent, true)

fun <T : ViewBinding> bindListItem(type: KClass<T>, parent: ViewGroup) = bindView(type, parent, false)

inline fun <reified T : ViewBinding> bindListItem(parent: ViewGroup) = bindView<T>(parent, false)

inline fun <reified T : ViewModel> viewModel(owner: ViewModelStoreOwner): T {
    return ViewModelProvider(owner)[T::class.java]
}