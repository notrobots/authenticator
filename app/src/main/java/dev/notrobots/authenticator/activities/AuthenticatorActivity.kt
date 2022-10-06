package dev.notrobots.authenticator.activities

import android.view.WindowManager
import androidx.preference.PreferenceManager
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.preferences2.getAllowScreenshots

open class AuthenticatorActivity : BaseActivity() {
    private val preferences by lazy {   //xxx Protected??
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onResume() {
        super.onResume()

        if (preferences.getAllowScreenshots()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}