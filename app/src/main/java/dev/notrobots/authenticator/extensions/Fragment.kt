package dev.notrobots.authenticator.extensions

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

fun <T : Fragment> KClass<T>.newInstance(factory: T.() -> Unit = {}): T {
    return createInstance().apply(factory)
}

/**
 * Sets the [androidx.fragment.app.FragmentResultListener] for the specified fragment.
 *
 * The fragment's class's qualified name will be used for as requestKey.
 */
inline fun <reified T : Fragment> Fragment.setFragmentResultListener(
    noinline listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
    setFragmentResultListener(T::class.qualifiedName!!, listener)
}

/**
 * Sets the given [result] for the specified fragment.
 *
 * The fragment's class's qualified name will be used for as requestKey.
 */
inline fun <reified T : Fragment> Fragment.setFragmentResult(result: Bundle = bundleOf()) {
    setFragmentResult(T::class.qualifiedName!!, result)
}

