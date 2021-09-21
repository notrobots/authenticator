package dev.notrobots.authenticator.extensions

import androidx.lifecycle.MutableLiveData

operator fun <T> MutableLiveData<T>.invoke(): T? {
    return value
}

operator fun <T> MutableLiveData<T>.invoke(value: T) {
    setValue(value)
}