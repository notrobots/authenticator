package dev.notrobots.authenticator.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Returns the current value.
 *
 * Note that calling this method on a background thread does not guarantee that the latest value set will be received.
 */
operator fun <T> LiveData<T>.invoke(): T? {
    return value
}

/**
 * Sets the value. If there are active observers, the value will be dispatched to them.
 * This method must be called from the main thread.
 */
operator fun <T> MutableLiveData<T>.invoke(newValue: T) {
    value = newValue
}