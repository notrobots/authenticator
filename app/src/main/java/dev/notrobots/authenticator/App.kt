package dev.notrobots.authenticator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.notrobots.androidstuff.util.LogUtil

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        LogUtil.setTag(TAG)
    }

    companion object {
        const val TAG = "OTP"
    }
}