package com.kidshield.pro.ui

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.kidshield.pro.security.BiometricHelper
import com.kidshield.pro.service.KidShieldAccessibilityService
import com.kidshield.pro.service.KidShieldTimerService
import com.kidshield.pro.service.LockOverlayManager

class BiometricUnlockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_UNLOCK_OVERLAY = "unlock_overlay"
        const val MODE_ANTI_UNINSTALL = "anti_uninstall"
    }

    private var unlockedSuccessfully = false
    private var mode: String = MODE_UNLOCK_OVERLAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_UNLOCK_OVERLAY

        val title = if (mode == MODE_ANTI_UNINSTALL) {
            "Anti-Uninstall Security / अनइंस्टॉल सुरक्षा"
        } else {
            "Parent Fingerprint Unlock"
        }

        val subtitle = if (mode == MODE_ANTI_UNINSTALL) {
            "Scan Parent Fingerprint to access KidShield settings or uninstall"
        } else {
            "Only Parent Fingerprint can unlock this device / केवल फिंगरप्रिंट से खोलें"
        }

        BiometricHelper.authenticateFingerprintOnly(
            activity = this,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                unlockedSuccessfully = true
                if (mode == MODE_ANTI_UNINSTALL) {
                    // Allow parent 60 seconds in Android Settings
                    KidShieldAccessibilityService.parentSettingsBypassUntilMs =
                        System.currentTimeMillis() + 60_000L
                    Toast.makeText(
                        this,
                        "Parent Verified • Settings Unlocked for 60s",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    KidShieldTimerService.unlockByParent(this)
                    Toast.makeText(
                        this,
                        "Unlocked by Parent Fingerprint / फ़ोन अनलॉक हो गया",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                finish()
            },
            onError = { err ->
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                if (mode == MODE_UNLOCK_OVERLAY) {
                    LockOverlayManager.restoreAfterBiometricCancel(this)
                }
                finish()
            }
        )
    }

    override fun onDestroy() {
        if (!unlockedSuccessfully && mode == MODE_UNLOCK_OVERLAY) {
            LockOverlayManager.restoreAfterBiometricCancel(this)
        }
        super.onDestroy()
    }
}
