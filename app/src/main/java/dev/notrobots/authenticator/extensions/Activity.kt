package dev.notrobots.authenticator.extensions

import android.app.Activity
import androidx.viewbinding.ViewBinding
import kotlin.reflect.full.declaredFunctions

inline fun <reified T : ViewBinding> Activity.bindView(): T {
    val method = T::class.declaredFunctions.find { it.name == "inflate" && it.parameters.size == 1 }

    if (method == null) {
        throw Exception("Cannot find method 'inflate(LayoutInflate)' for type ${T::class}")
    }

    return method.call(layoutInflater) as T
}