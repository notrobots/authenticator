package dev.notrobots.authenticator.activities

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.models.AppTheme
import dev.notrobots.preferences2.getAllowScreenshots
import dev.notrobots.preferences2.getAppTheme
import dev.notrobots.preferences2.getDynamicColors

open class AuthenticatorActivity : BaseActivity() {
    private val preferences by lazy {   //xxx Protected??
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private var currentTheme: AppTheme? = null
    private var dynamicColors: Boolean? = null
    protected var finishOnBackPressEnabled = false
        set(value) {
            field = value
            supportActionBar?.setDisplayHomeAsUpEnabled(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateTheme()
    }

    override fun onResume() {
        super.onResume()

        updateTheme(true)

        if (preferences.getAllowScreenshots()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && finishOnBackPressEnabled) {
            //TODO: Add a method to BaseActivity that checks execute the back press and checks if the double press to finish is enabled too
            onBackPressedDispatcher.onBackPressed()
            return true
        }

        return false
    }

    fun updateTheme(recreate: Boolean = false) {
        val theme = preferences.getAppTheme<AppTheme>()
        val dynamicColors = preferences.getDynamicColors()

        if (currentTheme != theme || (this.dynamicColors != dynamicColors && theme != AppTheme.Custom)) {
            currentTheme = theme
            this.dynamicColors = dynamicColors
            setTheme(theme, dynamicColors, recreate)
        }
    }

    fun setTheme(theme: AppTheme, dynamicColors: Boolean, recreate: Boolean = false) {
        val themeId = theme.id

        when (theme) {
            AppTheme.FollowSystem -> when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> setTheme(AppTheme.Dark, dynamicColors)
                Configuration.UI_MODE_NIGHT_NO -> setTheme(AppTheme.Light, dynamicColors)
            }
            AppTheme.Light,
            AppTheme.Dark,
            AppTheme.PitchBlack -> {
                setTheme(themeId)
            }
            AppTheme.Custom -> {
                //TODO: Use the custom theme
                setTheme(AppTheme.Light.id)
                makeToast("Custom theme not implement yet\nFalling back to light theme", Toast.LENGTH_LONG)
            }
        }

        if (dynamicColors && theme != AppTheme.Custom) {
            DynamicColors.applyIfAvailable(this)
        }

        if (recreate) {
            recreate()
        }
    }
}