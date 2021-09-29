package dev.notrobots.authenticator.extensions

import androidx.lifecycle.MutableLiveData

operator fun <T> MutableLiveData<T>.invoke(value: T) {
    setValue(value)
}

fun <T> MutableLiveData<T>.update(block: (T) -> Unit) {
    value?.let {
        block(it)
        value = it
    }
}