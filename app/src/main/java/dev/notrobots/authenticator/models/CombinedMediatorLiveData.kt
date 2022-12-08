package dev.notrobots.authenticator.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

open class CombinedMediatorLiveData<R>(
    vararg liveData: LiveData<*>,
    private val combine: (list: List<Any?>) -> R
) : MediatorLiveData<R>() {
    private val liveDataList: MutableList<Any?> = MutableList(liveData.size) { null }

    init {
        for (i in liveData.indices) {
            super.addSource(liveData[i]) {
                liveDataList[i] = it
                value = combine(liveDataList)
            }
        }
    }
}