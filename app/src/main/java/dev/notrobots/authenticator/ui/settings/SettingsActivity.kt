package dev.notrobots.authenticator.ui.settings

import android.os.Bundle
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {
    private val binding: ActivitySettingsBinding by viewBindings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(binding.container.id, MainSettingsFragment())
            .commit()
    }
}