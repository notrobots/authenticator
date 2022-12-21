package dev.notrobots.authenticator.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.annotation.StyleRes
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
    private val logger = Logger(this)
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
        if (theme == AppTheme.Custom) {
            logger.logw("Calling setTheme with AppTheme.Custom")
        } else {
            val nightMode = when (theme) {
                AppTheme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                AppTheme.Dark,
                AppTheme.PitchBlack -> AppCompatDelegate.MODE_NIGHT_YES
                AppTheme.FollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            setTheme(
                R.style.AppTheme,
                theme.id,
                nightMode,
                false,  // TrueBlack overlay is only used for custom themes
                dynamicColors
            )
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
        setTheme(
            R.style.AppTheme,
            theme.id,
            nightMode,
            trueBlack,
            false
        )

        if (recreate) {
            recreate()
        }
    }

    /**
     * Sets the given [baseTheme] and then applies [overlayTheme] on top of it.
     *
     * @param baseTheme The base theme to use.
     * @param overlayTheme The overlay theme to apply, this should hold the actual theme's colors.
     * @param nightMode One of [NightMode].
     * @param trueBlack Whether or not true black style is applied.
     * @param dynamicColors Whether or not dynamic colors are applied, only works on android 13 and higher.
     */
    private fun setTheme(
        @StyleRes baseTheme: Int,
        @StyleRes overlayTheme: Int,
        @NightMode nightMode: Int,
        trueBlack: Boolean,
        dynamicColors: Boolean
    ) {
        AppCompatDelegate.setDefaultNightMode(nightMode)

        setTheme(baseTheme)
        this.theme.applyStyle(overlayTheme, true)

        if (trueBlack) {
            this.theme.applyStyle(R.style.AppThemeOverlay_TrueBlack, true)
        }

        if (dynamicColors) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
    }
}