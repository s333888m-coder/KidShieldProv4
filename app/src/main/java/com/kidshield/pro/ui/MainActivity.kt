package com.kidshield.pro.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kidshield.pro.data.MonitoredApp
import com.kidshield.pro.data.PrefsManager
import com.kidshield.pro.security.BiometricHelper
import com.kidshield.pro.security.KidShieldDeviceAdminReceiver
import com.kidshield.pro.security.SoundAlertManager
import com.kidshield.pro.service.KidShieldAccessibilityService
import com.kidshield.pro.service.KidShieldTimerService
import com.kidshield.pro.ui.screens.DashboardScreen
import com.kidshield.pro.ui.screens.FingerprintGateScreen
import com.kidshield.pro.ui.screens.SettingsScreen
import com.kidshield.pro.ui.theme.KidShieldProTheme

class MainActivity : FragmentActivity() {

    private lateinit var prefs: PrefsManager

    private var isUnlocked by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    private var fpError by mutableStateOf<String?>(null)

    private var monitoredApps by mutableStateOf<List<MonitoredApp>>(emptyList())
    private var installedApps by mutableStateOf<List<MonitoredApp>>(emptyList())
    private var selectedPackage by mutableStateOf("com.google.android.youtube")
    private var timerMinutes by mutableIntStateOf(15)
    private var timerSeconds by mutableIntStateOf(0)
    private var afterTimerMinutes by mutableIntStateOf(30)
    private var soundName by mutableStateOf("Default Alarm")
    private var antiUninstallGuard by mutableStateOf(true)
    private var autoForceStopEnabled by mutableStateOf(true)

    private var hasOverlay by mutableStateOf(false)
    private var hasAccessibility by mutableStateOf(false)
    private var hasDeviceAdmin by mutableStateOf(false)
    private var hasNotification by mutableStateOf(true)

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val displayName = resolveFileName(uri)
            prefs.customSoundUri = uri.toString()
            prefs.customSoundName = displayName
            soundName = displayName
            Toast.makeText(this, "Sound Saved: $displayName", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotification = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)
        loadState()
        installedApps = loadLaunchableInstalledApps()

        setContent {
            KidShieldProTheme {
                LaunchedEffect(Unit) {
                    if (!isUnlocked) {
                        triggerParentFingerprintScan()
                    }
                }

                if (!isUnlocked) {
                    FingerprintGateScreen(
                        errorMessage = fpError,
                        onScanClick = { triggerParentFingerprintScan() }
                    )
                } else if (showSettings) {
                    SettingsScreen(
                        afterTimerMinutes = afterTimerMinutes,
                        antiUninstallGuard = antiUninstallGuard,
                        autoForceStopEnabled = autoForceStopEnabled,
                        hasOverlayPermission = hasOverlay,
                        hasAccessibilityPermission = hasAccessibility,
                        hasDeviceAdminPermission = hasDeviceAdmin,
                        hasNotificationPermission = hasNotification,
                        onBack = { showSettings = false },
                        onSelectAfterTimerMinutes = { mins ->
                            afterTimerMinutes = mins
                            prefs.afterTimerProtectionMinutes = mins
                        },
                        onToggleAntiUninstallGuard = { enabled ->
                            antiUninstallGuard = enabled
                            prefs.isAntiUninstallGuardEnabled = enabled
                        },
                        onToggleAutoForceStop = { enabled ->
                            autoForceStopEnabled = enabled
                            prefs.isAutoForceStopEnabled = enabled
                        },
                        onRequestOverlay = { requestOverlayPermission() },
                        onRequestAccessibility = { requestAccessibilityPermission() },
                        onRequestDeviceAdmin = { requestDeviceAdminPermission() },
                        onRequestNotification = { requestNotificationPermission() }
                    )
                } else {
                    DashboardScreen(
                        monitoredApps = monitoredApps,
                        selectedPackage = selectedPackage,
                        installedApps = installedApps,
                        timerMinutes = timerMinutes,
                        timerSeconds = timerSeconds,
                        soundName = soundName,
                        afterTimerLabel = formatAfterTimerLabel(afterTimerMinutes),
                        allPermissionsReady = hasOverlay && hasAccessibility && hasDeviceAdmin,
                        onSelectApp = { pkg ->
                            selectedPackage = pkg
                            prefs.selectedLaunchPackage = pkg
                        },
                        onAddApp = { app ->
                            monitoredApps = prefs.addMonitoredApp(app)
                            selectedPackage = app.packageName
                            prefs.selectedLaunchPackage = app.packageName
                        },
                        onRemoveApp = { pkg ->
                            monitoredApps = prefs.removeMonitoredApp(pkg)
                            selectedPackage = prefs.selectedLaunchPackage
                        },
                        onTimerChanged = { m, s ->
                            timerMinutes = m
                            timerSeconds = s
                            prefs.timerMinutes = m
                            prefs.timerSeconds = s
                        },
                        onPickSound = {
                            soundPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        onTestSound = {
                            SoundAlertManager.playAlertSound(this, 3500L)
                        },
                        onOpenSettings = { showSettings = true },
                        onActivateClick = { activateKidsSession() },
                        onLockAppNow = {
                            isUnlocked = false
                            fpError = null
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    override fun onStop() {
        super.onStop()
        // Always auto-lock KidShield Pro dashboard when leaving app (unless picking sound)
        if (!isChangingConfigurations) {
            SoundAlertManager.stopSound()
        }
    }

    private fun loadState() {
        monitoredApps = prefs.getMonitoredApps()
        selectedPackage = prefs.selectedLaunchPackage
        timerMinutes = prefs.timerMinutes
        timerSeconds = prefs.timerSeconds
        afterTimerMinutes = prefs.afterTimerProtectionMinutes
        soundName = prefs.customSoundName
        antiUninstallGuard = prefs.isAntiUninstallGuardEnabled
        autoForceStopEnabled = prefs.isAutoForceStopEnabled
        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        hasOverlay = Settings.canDrawOverlays(this)
        hasAccessibility = isAccessibilityEnabled()
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        hasDeviceAdmin = dpm.isAdminActive(KidShieldDeviceAdminReceiver.getComponentName(this))
        hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun triggerParentFingerprintScan() {
        fpError = null
        BiometricHelper.authenticateFingerprintOnly(
            activity = this,
            title = "KidShield Pro • Parent Access",
            subtitle = "Scan your fingerprint to enter / फिंगरप्रिंट से खोलें",
            onSuccess = {
                isUnlocked = true
                fpError = null
            },
            onError = { err ->
                fpError = err
            }
        )
    }

    private fun activateKidsSession() {
        if (!hasOverlay) {
            Toast.makeText(
                this,
                "Please enable Display Over Other Apps permission first!",
                Toast.LENGTH_LONG
            ).show()
            showSettings = true
            return
        }

        val totalSeconds = (timerMinutes * 60L) + timerSeconds.toLong()
        if (totalSeconds <= 0L) {
            Toast.makeText(
                this,
                "Please set a timer greater than 0 seconds!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 1. Start Foreground Countdown & Protection Service
        KidShieldTimerService.startTimerSession(this, totalSeconds)

        // 2. Immediately lock KidShield Pro dashboard
        isUnlocked = false
        showSettings = false

        // 3. Launch selected kids app (e.g., YouTube) and exit KidShield
        val launchIntent = packageManager.getLaunchIntentForPackage(selectedPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Toast.makeText(
                this,
                "Timer ACTIVE! Selected app not installed, going to Home.",
                Toast.LENGTH_SHORT
            ).show()
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
        finish()
    }

    private fun requestOverlayPermission() {
        KidShieldAccessibilityService.parentSettingsBypassUntilMs = System.currentTimeMillis() + 60_000L
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestAccessibilityPermission() {
        KidShieldAccessibilityService.parentSettingsBypassUntilMs = System.currentTimeMillis() + 60_000L
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestDeviceAdminPermission() {
        KidShieldAccessibilityService.parentSettingsBypassUntilMs = System.currentTimeMillis() + 60_000L
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                KidShieldDeviceAdminReceiver.getComponentName(this@MainActivity)
            )
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Protects KidShield Pro from unauthorized uninstall by children."
            )
        }
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun loadLaunchableInstalledApps(): List<MonitoredApp> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = packageManager.queryIntentActivities(intent, 0)
        return resolved
            .map { info ->
                MonitoredApp(
                    packageName = info.activityInfo.packageName,
                    appName = info.loadLabel(packageManager).toString()
                )
            }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    private fun resolveFileName(uri: Uri): String {
        var name = "Custom Sound"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {
        }
        return name
    }

    private fun formatAfterTimerLabel(mins: Int): String {
        return when (mins) {
            PrefsManager.AFTER_TIMER_UNTIL_FP -> "Until Fingerprint"
            60 -> "1 Hour"
            120 -> "2 Hours"
            else -> "$mins Min"
        }
    }
}
