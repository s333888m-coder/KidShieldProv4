package com.kidshield.pro.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kidshield.pro.data.PrefsManager
import com.kidshield.pro.ui.BiometricUnlockActivity

class KidShieldAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: KidShieldAccessibilityService? = null
            private set

        @Volatile
        var parentSettingsBypassUntilMs: Long = 0L

        private const val STEP_IDLE = 0
        private const val STEP_FIND_FORCE_STOP = 1
        private const val STEP_CONFIRM_DIALOG = 2

        @Volatile
        private var autoForceStopStep: Int = STEP_IDLE

        @Volatile
        private var autoForceStopDeadlineMs: Long = 0L

        fun isAutoForceStopRunning(): Boolean {
            return autoForceStopStep != STEP_IDLE && System.currentTimeMillis() < autoForceStopDeadlineMs
        }
    }

    private val prefs: PrefsManager by lazy { PrefsManager(this) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var targetForceStopPkg: String? = null

    private val settingsPackages = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.miui.securitycenter",
        "com.samsung.android.packageinstaller",
        "com.oplus.appmanager",
        "com.coloros.appmanager"
    )

    private val forceStopLabels = listOf(
        "Force stop",
        "ज़बरन रोकें",
        "फोर्स स्टॉप",
        "बलपूर्वक रोकें",
        "Force Stop"
    )

    private val confirmLabels = listOf(
        "OK",
        "Force stop",
        "ठीक है",
        "ज़बरन रोकें",
        "फोर्स स्टॉप"
    )

    private val forceStopTimeoutRunnable = Runnable {
        finishAutoForceStop()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    fun performHomeAction() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Automatically opens the target app's Android App Info screen, taps Force Stop + OK,
     * and returns immediately to Home while our 100% opaque Fake PIN Lock covers the screen.
     */
    fun startAutoForceStop(packageName: String) {
        mainHandler.post {
            targetForceStopPkg = packageName
            autoForceStopStep = STEP_FIND_FORCE_STOP
            autoForceStopDeadlineMs = System.currentTimeMillis() + 4500L

            mainHandler.removeCallbacks(forceStopTimeoutRunnable)
            mainHandler.postDelayed(forceStopTimeoutRunnable, 4500L)

            try {
                val appInfoIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                }
                startActivity(appInfoIntent)
            } catch (_: Exception) {
                finishAutoForceStop()
            }
        }
    }

    private fun finishAutoForceStop() {
        mainHandler.removeCallbacks(forceStopTimeoutRunnable)
        val pkgToKill = targetForceStopPkg
        autoForceStopStep = STEP_IDLE
        autoForceStopDeadlineMs = 0L
        targetForceStopPkg = null

        // Return cleanly to Home screen
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Also run killBackgroundProcesses as extra safety
        if (!pkgToKill.isNullOrBlank()) {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.killBackgroundProcesses(pkgToKill)
            } catch (_: Exception) {
            }
        }

        // Guarantee 100% solid Fake PIN Overlay stays on top
        if (prefs.isProtectionCurrentlyActive()) {
            LockOverlayManager.showFakePinOverlay(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Handle automated Force Stop flow first
        if (isAutoForceStopRunning()) {
            if (settingsPackages.contains(pkg)) {
                handleAutoForceStopWindow()
            }
            return
        }

        // Ignore our own app & SystemUI biometric dialog
        if (pkg == packageName || pkg == "com.android.systemui") return

        // 1. If After-Timer Protection Lock is currently enforced -> always keep Fake PIN Overlay on top!
        if (prefs.isProtectionCurrentlyActive()) {
            LockOverlayManager.showFakePinOverlay(this)
            return
        }

        // 2. Anti-Uninstall & Settings Guard
        if (prefs.isAntiUninstallGuardEnabled &&
            settingsPackages.contains(pkg) &&
            System.currentTimeMillis() > parentSettingsBypassUntilMs
        ) {
            val windowText = buildString {
                event.text?.forEach { append(it).append(" ") }
                rootInActiveWindow?.let { root ->
                    val nodes = root.findAccessibilityNodeInfosByText("KidShield")
                    if (!nodes.isNullOrEmpty()) append("KidShield ")
                }
            }

            val isTargetingKidShield = windowText.contains("KidShield", ignoreCase = true) ||
                windowText.contains("Device admin", ignoreCase = true) ||
                (windowText.contains("Uninstall", ignoreCase = true) &&
                    windowText.contains("Pro", ignoreCase = true))

            if (isTargetingKidShield) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                val gateIntent = Intent(this, BiometricUnlockActivity::class.java).apply {
                    putExtra(BiometricUnlockActivity.EXTRA_MODE, BiometricUnlockActivity.MODE_ANTI_UNINSTALL)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(gateIntent)
            }
        }
    }

    private fun handleAutoForceStopWindow() {
        val root = rootInActiveWindow ?: return

        if (autoForceStopStep == STEP_FIND_FORCE_STOP) {
            // Look for Force Stop button by label or common Settings view IDs
            val forceStopNode = findButtonNode(
                root = root,
                labels = forceStopLabels,
                viewIds = listOf(
                    "com.android.settings:id/right_button",
                    "com.android.settings:id/force_stop_button",
                    "com.android.settings:id/button3"
                )
            )
            if (forceStopNode != null) {
                if (!forceStopNode.isEnabled) {
                    // Already force-stopped! Exit cleanly.
                    finishAutoForceStop()
                    return
                }
                if (clickNodeOrParent(forceStopNode)) {
                    autoForceStopStep = STEP_CONFIRM_DIALOG
                }
            }
        } else if (autoForceStopStep == STEP_CONFIRM_DIALOG) {
            // Confirm dialog positive button (android:id/button1 or OK / Force stop)
            val confirmNode = findButtonNode(
                root = root,
                labels = confirmLabels,
                viewIds = listOf(
                    "android:id/button1",
                    "com.android.settings:id/button1"
                )
            )
            if (confirmNode != null && clickNodeOrParent(confirmNode)) {
                autoForceStopStep = STEP_IDLE
                mainHandler.postDelayed({ finishAutoForceStop() }, 220L)
            }
        }
    }

    private fun findButtonNode(
        root: AccessibilityNodeInfo,
        labels: List<String>,
        viewIds: List<String>
    ): AccessibilityNodeInfo? {
        for (label in labels) {
            val found = root.findAccessibilityNodeInfosByText(label)
            if (!found.isNullOrEmpty()) {
                val best = found.firstOrNull { node ->
                    val t = node.text?.toString()?.trim() ?: ""
                    val desc = node.contentDescription?.toString()?.trim() ?: ""
                    t.equals(label, ignoreCase = true) || desc.equals(label, ignoreCase = true)
                } ?: found.firstOrNull()
                if (best != null) return best
            }
        }
        for (id in viewIds) {
            val found = root.findAccessibilityNodeInfosByViewId(id)
            if (!found.isNullOrEmpty()) {
                return found.firstOrNull()
            }
        }
        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 4) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            depth++
        }
        return false
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        instance = null
        super.onDestroy()
    }
}
