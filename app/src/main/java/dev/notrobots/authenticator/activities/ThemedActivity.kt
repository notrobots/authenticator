package dev.notrobots.authenticator.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat

abstract class ThemedActivity : BaseActivity() {
    protected fun setFullscreen() {
        hideActionBar()
        hideStatusBar()
    }

    protected fun hideActionBar() {
        actionBar?.hide()
        supportActionBar?.hide()
    }

    @SuppressLint("ObsoleteSdkInt")
    protected fun hideStatusBar() {
        when {
            // -- R ⬆⬆ --
            // From Android R and upwards we use the WindowInsetsController
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                window.insetsController?.hide(WindowInsetsCompat.Type.statusBars())
            }

            // -- Lollipop ⬆⬆ --
            // From Android Lollipop we use the same flags as KitKat but we also
            // need to change the status bar color to TRANSPARENT
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                window.statusBarColor = Color.TRANSPARENT
            }

            // -- KitKat ⬆⬆ --
            // From Android R and upwards we use the WindowInsetsController
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                )
            }
        }

        // -- Pre KitKat --
        // In older devices, we use the FLAG_FULLSCREEN flag to hide the status bar
        // We also do this for all remaining devices to make sure the full screen works
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    protected fun disableTitle() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }
}