package dev.notrobots.authenticator.extensions

import android.content.Context
import android.util.TypedValue
import androidx.biometric.BiometricManager

/**
 * Checks if the device is secured.
 *
 * By default it checks all possible authenticators, specify [authenticators] to change this behaviour.
 */
fun Context.isDeviceSecured(authenticators: Int = -1): Boolean {
    val biometricManager = BiometricManager.from(this)

    val authenticator = biometricManager.canAuthenticate(
        if (authenticators != -1) {
            authenticators
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    )

    return authenticator == BiometricManager.BIOMETRIC_SUCCESS
}