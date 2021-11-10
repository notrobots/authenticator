package dev.notrobots.authenticator.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Coroutines {
    fun coroutine(work: suspend (() -> Unit)) {
        CoroutineScope(Dispatchers.Main).launch {
            work()
        }
    }
}