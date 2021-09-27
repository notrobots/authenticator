package dev.notrobots.authenticator.util

import android.util.Log
import dev.notrobots.authenticator.App

fun logd(content: Any? = "") {
    Log.d(App.TAG, content.toString())
}

fun loge(error: Any? = "") {
    Log.e(App.TAG, error.toString())
}