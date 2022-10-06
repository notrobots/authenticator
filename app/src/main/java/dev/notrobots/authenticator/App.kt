package dev.notrobots.authenticator

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import dev.notrobots.androidstuff.util.LogUtil

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)  //TODO: Remove after you're done testing
        LogUtil.setTag(TAG)
    }

    companion object {
        const val TAG = "OTP"
    }
}