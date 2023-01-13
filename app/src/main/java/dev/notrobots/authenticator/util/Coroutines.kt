package dev.notrobots.authenticator.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Coroutines {
    fun coroutine(work: suspend (() -> Unit)) {
        //FIXME: Bad practice. https://stackoverflow.com/a/59060997/15872880
        CoroutineScope(Dispatchers.Main).launch {
            work()
        }
    }
}