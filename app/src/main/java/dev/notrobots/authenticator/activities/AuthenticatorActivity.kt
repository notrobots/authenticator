package dev.notrobots.authenticator.activities

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.AppTheme
import dev.notrobots.authenticator.models.CustomAppTheme
import dev.notrobots.preferences2.*

open class AuthenticatorActivity : BaseActivity() {
    private val preferences by lazy {   //xxx Protected??
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private var appTheme: AppTheme? = null
    private var dynamicColors: Boolean = false
    private var customTheme: CustomAppTheme? = null
    private var customThemeTrueBlack: Boolean = false
    private var customThemeNightMode: Int = 0
    protected var finishOnBackPressEnabled = false
        set(value) {
            field = value
            supportActionBar?.setDisplayHomeAsUpEnabled(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The activity's content has not been created yet,
        // so there's no need to recreate the activity
        updateTheme()
    }

    override fun onResume() {
        super.onResume()

        // If there were any theme changes, this will recreate the activity
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

    /**
     * Updates the theme based on the user's preferences and optionally recreates the activity if specified.
     *
     * This should only be called inside the [onCreate] and [onResume] methods or when the theme settings
     * have been changed.
     */
    fun updateTheme(recreate: Boolean = false) {
        val appTheme = preferences.getAppTheme<AppTheme>()
        val dynamicColors = preferences.getDynamicColors()
        val nightMode = preferences.getCustomAppThemeNightMode()
        val trueBlack = preferences.getCustomAppThemeTrueBlack()
        val customTheme = preferences.getCustomAppTheme<CustomAppTheme>()

        if (appTheme == AppTheme.Custom) {
            if (this.appTheme != appTheme ||
                this.customTheme != customTheme ||
                this.customThemeNightMode != nightMode ||
                this.customThemeTrueBlack != trueBlack
            ) {
                this.appTheme = appTheme
                this.customTheme = customTheme
                this.customThemeNightMode = nightMode
                this.customThemeTrueBlack = trueBlack

                setCustomTheme(
                    this.customTheme!!,
                    this.customThemeNightMode,
                    this.customThemeTrueBlack,
                    recreate
                )
            }
        } else {
            if (this.appTheme != appTheme || this.dynamicColors != dynamicColors) {
                this.appTheme = appTheme
                this.dynamicColors = dynamicColors

                setTheme(this.appTheme!!, this.dynamicColors, recreate)
            }
        }
    }

    /**
     * Sets the given [theme] and optionally recreates the activity if specified.
     *
     * This assumes that the current theme config differ from the one stored in the preferences.
     */
    fun setTheme(theme: AppTheme, dynamicColors: Boolean, recreate: Boolean = false) {
        val themeId = theme.id

        if (theme == AppTheme.FollowSystem) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> setTheme(AppTheme.Dark, dynamicColors)
                Configuration.UI_MODE_NIGHT_NO -> setTheme(AppTheme.Light, dynamicColors)
            }
        } else {
            setTheme(themeId)

            if (dynamicColors) {
                DynamicColors.applyToActivityIfAvailable(this)
            }
        }

        if (recreate) {
            recreate()
        }
    }

    /**
     * Sets the given custom [theme] and optionally recreates the activity if specified.
     *
     * This assumes that the current theme config differ from the one stored in the preferences.
     */
    fun setCustomTheme(theme: CustomAppTheme, @NightMode nightMode: Int, trueBlack: Boolean, recreate: Boolean = false) {
        val currentNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)

        setTheme(R.style.Theme_Custom)
        this.theme.applyStyle(theme.id, true)

        //fixme: Pitch black can be applied regardless of theme, it will be applied only in dark mode
        // since the light version doesn't define its attributes
        if (((nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && currentNightMode == Configuration.UI_MODE_NIGHT_YES) || nightMode == AppCompatDelegate.MODE_NIGHT_YES) && trueBlack) {
            this.theme.applyStyle(R.style.ThemeOverlay_TrueBlack, true)
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)

        if (recreate) {
            recreate()
        }
    }
}