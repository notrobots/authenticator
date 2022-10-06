package dev.notrobots.authenticator.ui.login

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityLoginBinding
import dev.notrobots.authenticator.extensions.isDeviceSecured
import dev.notrobots.authenticator.extensions.showBiometricPrompt
import dev.notrobots.authenticator.ui.accountlist.AccountListActivity
import dev.notrobots.authenticator.ui.accountlist.AccountListAdapter
import dev.notrobots.preferences2.getAppLock
import dev.notrobots.preferences2.putAppLock

class LoginActivity : AuthenticatorActivity() {
    private val binding by viewBindings<ActivityLoginBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        if (preferences.getAppLock()) {
            if (isDeviceSecured()) {
                setContentView(binding.root)

                binding.unlock.setOnClickListener { login() }
                login()
            } else {
                preferences.putAppLock(false)
                onLoginSucceed()

                makeToast("No device lock configured\nApp lock will be disabled")
                logd("No device lock configured\nApp lock will be disabled")
            }
        } else {
            onLoginSucceed()
            logd("App lock is disabled. No need to authenticate")
        }
    }

    private fun login() {
        showBiometricPrompt(
            "Authenticator",
            null,
            onSuccess = {
                onLoginSucceed()
            }
        )
    }

    private fun onLoginSucceed() {
        startActivity(AccountListActivity::class)
        finish()
    }
}