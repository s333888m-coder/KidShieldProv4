package com.kidshield.pro.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    // Strictly Fingerprint/Biometric ONLY — NO DEVICE_CREDENTIAL (PIN/Pattern) fallback!
    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun checkBiometricStatus(activity: FragmentActivity): Int {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(ALLOWED_AUTHENTICATORS)
    }

    fun authenticateFingerprintOnly(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val status = checkBiometricStatus(activity)
        if (status != BiometricManager.BIOMETRIC_SUCCESS) {
            val msg = when (status) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "No fingerprint enrolled! Please add a fingerprint in Android Settings."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    "No fingerprint sensor found on this device."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "Fingerprint sensor is currently unavailable."
                else -> "Biometric authentication unavailable (Code: $status)"
            }
            onError(msg)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Fingerprint didn't match; prompt stays open for retry
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel / रद्द करें")
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
