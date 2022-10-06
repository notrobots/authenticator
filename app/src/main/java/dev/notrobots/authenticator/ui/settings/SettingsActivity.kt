package dev.notrobots.authenticator.ui.settings

import android.os.Bundle
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {
    private val binding: ActivitySettingsBinding by viewBindings()
    private val toolbar by lazy {
        binding.toolbarLayout.toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(binding.root)

        supportFragmentManager
            .beginTransaction()
            .replace(binding.container.id, MainSettingsFragment())
            .commit()
    }
}