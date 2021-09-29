package dev.notrobots.authenticator.extensions

import androidx.lifecycle.LiveData

operator fun <T> LiveData<T>.invoke(): T? {
    return value
}