package dev.notrobots.authenticator.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner

/**
 * Sets the [androidx.fragment.app.FragmentResultListener] for the specified fragment.
 *
 * The fragment's class's qualified name will be used for as requestKey.
 */
inline fun <reified T : Fragment> FragmentManager.setFragmentResultListener(
    lifecycleOwner: LifecycleOwner,
    noinline listener: ((requestKey: String, bundle: Bundle) -> Unit)
) {
    setFragmentResultListener(T::class.qualifiedName!!, lifecycleOwner, listener)
}