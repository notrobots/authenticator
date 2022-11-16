package dev.notrobots.authenticator.extensions

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.ui.backupexport.ExportActivity

/**
 * Shows the biometric prompt to the user.
 */
internal fun FragmentActivity.showBiometricPrompt(
    title: String,
    subtitle: String?,
    onSuccess: (result: BiometricPrompt.AuthenticationResult) -> Unit,
    onFailure: () -> Unit = {},
    onError: (errString: CharSequence) -> Unit = {},
    authenticators: Int = -1
) {
    val executor = ContextCompat.getMainExecutor(this)
    val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onError(errString)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess(result)
        }

        override fun onAuthenticationFailed() {
            onFailure()
        }
    })
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(
            if (authenticators != -1) {
                authenticators
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
        )
        .build()

    biometricPrompt.authenticate(promptInfo)
}

/**
 * Shows the biometric prompt to the user and launches the [ExportActivity]
 * on authentication succeeded.
 */
internal fun FragmentActivity.requestExport(requireAuthentication: Boolean, isSecured: Boolean) {
    if (requireAuthentication) {
        if (isSecured) {
            showBiometricPrompt(
                "Authenticator",
                "Unlock to backup your data",
                onSuccess = {
                    startActivity(ExportActivity::class)
                }
            )
        } else {
            makeToast("No device lock configured\nExport lock will be disabled")
            startActivity(ExportActivity::class)
        }
    } else {
        startActivity(ExportActivity::class)
    }
}