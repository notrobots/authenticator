package dev.notrobots.authenticator.util

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import dev.notrobots.authenticator.extensions.toUri
import org.apache.commons.codec.binary.Base32
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

fun <T, B : ViewBinding> adapterOf(
    context: Context,
    iterable: Iterable<T>,
    bindingType: KClass<B>,
    rowFactory: (item: T, binding: B) -> View
): ArrayAdapter<T> {
    return object : ArrayAdapter<T>(context, 0) {
        override fun getItem(position: Int): T {
            return iterable.elementAt(position)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val binding by lazy { bindView(bindingType, parent) }

            return (convertView ?: binding.root).also {
                rowFactory(getItem(position), binding)
            }
        }
    }
}

fun <T> adapterOf(
    context: Context,
    iterable: Iterable<T>,
    @LayoutRes layout: Int,
    rowFactory: (item: T, view: View) -> View
): ArrayAdapter<T> {
    val inflater by lazy {
        LayoutInflater.from(context)
    }

    return object : ArrayAdapter<T>(context, 0) {
        override fun getItem(position: Int): T {
            return iterable.elementAt(position)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(layout, parent, false)

            rowFactory(getItem(position), view)

            return view
        }
    }
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

inline fun <reified T : ViewBinding> bindView(
    parent: ViewGroup,
    attachToRoot: Boolean = false
): T {
    return bindView(T::class, parent, attachToRoot)
}

fun <T : ViewBinding> bindView(
    type: KClass<T>,
    parent: ViewGroup,
    attachToRoot: Boolean = false
): T {
    val inflate = type.declaredFunctions.find {
        it.name == "inflate" &&
        it.parameters.size == 3 &&
        it.parameters[0].type.classifier == LayoutInflater::class &&
        it.parameters[1].type.classifier == ViewGroup::class &&
        it.parameters[2].type.classifier == Boolean::class
    } ?: throw Exception("Cannot find method 'inflate(LayoutInflater, ViewGroup, Boolean)'")
    val layoutInflater = LayoutInflater.from(parent.context)

    return inflate.call(layoutInflater, parent, attachToRoot) as T
}

inline fun <reified T : ViewBinding> viewBindings(
    parent: ViewGroup,
    attachToRoot: Boolean = false
): Lazy<T> {
    return lazy {
        bindView(parent, attachToRoot)
    }
}

fun <T : ViewBinding> viewBindings(
    type: KClass<T>,
    parent: ViewGroup,
    attachToRoot: Boolean = false
): Lazy<T> {
    return lazy {
        bindView(type, parent, attachToRoot)
    }
}

inline fun <reified T : ViewBinding> bindView(view: View): T {
    return bindView(T::class, view)
}

fun <T : ViewBinding> bindView(
    type: KClass<T>,
    view: View
): T {
    val inflate = type.declaredFunctions.find {
        it.name == "bind" &&
        it.parameters.size == 1 &&
        it.parameters[0].type.classifier == View::class
    } ?: throw Exception("Cannot find method 'bind(View)'")

    return inflate.call(view) as T
}

inline fun <reified T : ViewBinding> viewBindings(view: View): Lazy<T> {
    return lazy {
        bindView(view)
    }
}

fun <T : ViewBinding> viewBindings(type: KClass<T>, view: View): Lazy<T> {
    return lazy {
        bindView(type, view)
    }
}

fun daysToMillis(days: Int): Long {
    return TimeUnit.DAYS.toMillis(days.toLong())
}