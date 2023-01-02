package dev.notrobots.authenticator.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

fun PreferenceDialogFragmentCompat.show(parent: PreferenceFragmentCompat) {
    setTargetFragment(parent, 0)
    show(parent.parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
}

fun <T : PreferenceDialogFragmentCompat> KClass<T>.newInstance(
    key: String,
    factory: T.(bundle: Bundle) -> Unit = {}
): T {
    return createInstance().apply {
        val bundle = Bundle(1).apply {
            putString("key", key)
        }

        arguments = bundle
        factory(bundle)
    }
}