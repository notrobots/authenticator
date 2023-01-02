package dev.notrobots.authenticator.extensions

import androidx.fragment.app.Fragment
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

fun <T : Fragment> KClass<T>.newInstance(factory: T.() -> Unit = {}): T {
    return createInstance().apply(factory)
}