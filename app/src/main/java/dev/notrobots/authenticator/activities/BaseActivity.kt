package dev.notrobots.authenticator.activities

import androidx.appcompat.app.AppCompatActivity
import dev.notrobots.authenticator.extensions.makeToast

abstract class BaseActivity : AppCompatActivity() {
    private var backPressedTime = 0L
    protected var backPressedDelay = DEFAULT_BACK_PRESSED_DELAY

    override fun onBackPressed() {
        if (!isDoubleBackPressToExitEnabled() || supportFragmentManager.backStackEntryCount != 0) {
            super.onBackPressed()
            return
        }

        if (backPressedTime + backPressedDelay > System.currentTimeMillis()) {
            super.onBackPressed()
            return
        } else {
            makeToast("Press back again to exit")
        }

        backPressedTime = System.currentTimeMillis()
    }

    open fun isDoubleBackPressToExitEnabled() = false

    companion object {
        const val DEFAULT_BACK_PRESSED_DELAY = 1500
    }
}