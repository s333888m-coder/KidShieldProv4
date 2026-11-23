package com.kidshield.pro.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.kidshield.pro.KidShieldApp
import com.kidshield.pro.R
import com.kidshield.pro.data.PrefsManager
import com.kidshield.pro.security.MediaControlManager
import com.kidshield.pro.security.SoundAlertManager
import com.kidshield.pro.ui.MainActivity
import java.util.Locale

class KidShieldTimerService : Service() {

    companion object {
        const val ACTION_START_TIMER = "com.kidshield.pro.ACTION_START_TIMER"
        const val ACTION_RESUME_PROTECTION = "com.kidshield.pro.ACTION_RESUME_PROTECTION"
        const val ACTION_PARENT_UNLOCK = "com.kidshield.pro.ACTION_PARENT_UNLOCK"
        private const val NOTIFICATION_ID = 7001

        fun startTimerSession(context: Context, durationSeconds: Long) {
            val prefs = PrefsManager(context)
            val now = System.currentTimeMillis()
            prefs.isSessionActive = true
            prefs.isLockEnforced = false
            prefs.timerEndMillis = now + (durationSeconds * 1000L)

            val intent = Intent(context, KidShieldTimerService::class.java).apply {
                action = ACTION_START_TIMER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun unlockByParent(context: Context) {
            val intent = Intent(context, KidShieldTimerService::class.java).apply {
                action = ACTION_PARENT_UNLOCK
            }
            context.startService(intent)
        }
    }

    private lateinit var prefs: PrefsManager
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()

            if (prefs.isSessionActive && !prefs.isLockEnforced) {
                val remainingMs = prefs.timerEndMillis - now
                if (remainingMs <= 0L) {
                    triggerLockEnforcement()
                } else {
                    val sec = remainingMs / 1000L
                    val text = String.format(
                        Locale.getDefault(),
                        "Kids Timer Active: %02d:%02d remaining",
                        sec / 60,
                        sec % 60
                    )
                    updateNotification(text)
                    handler.postDelayed(this, 1000L)
                }
            } else if (prefs.isLockEnforced) {
                val endMs = prefs.protectionEndMillis
                if (endMs != -1L && now >= endMs) {
                    completeAndStopService()
                } else {
                    if (!LockOverlayManager.isOverlayShowing() &&
                        !KidShieldAccessibilityService.isAutoForceStopRunning()
                    ) {
                        LockOverlayManager.showFakePinOverlay(this@KidShieldTimerService)
                    }
                    val statusText = if (endMs == -1L) {
                        "Fake PIN Lock Active (Until Parent Fingerprint)"
                    } else {
                        val leftSec = ((endMs - now) / 1000L).coerceAtLeast(0L)
                        String.format(
                            Locale.getDefault(),
                            "Phone Protected: %02d:%02d left (Fingerprint Only)",
                            leftSec / 60,
                            leftSec % 60
                        )
                    }
                    updateNotification(statusText)
                    handler.postDelayed(this, 1000L)
                }
            } else {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PARENT_UNLOCK -> {
                SoundAlertManager.stopSound()
                completeAndStopService()
                return START_NOT_STICKY
            }
            ACTION_RESUME_PROTECTION -> {
                startForegroundSafe("KidShield Pro Protection Active")
                if (prefs.isProtectionCurrentlyActive()) {
                    MediaControlManager.silenceAndStopMedia(this)
                    LockOverlayManager.showFakePinOverlay(this)
                    handler.removeCallbacks(ticker)
                    handler.post(ticker)
                } else {
                    stopSelf()
                }
            }
            else -> {
                startForegroundSafe("KidShield Pro Timer Running...")
                handler.removeCallbacks(ticker)
                handler.post(ticker)
            }
        }
        return START_STICKY
    }

    private fun triggerLockEnforcement() {
        prefs.isSessionActive = false
        prefs.isLockEnforced = true

        val afterMinutes = prefs.afterTimerProtectionMinutes
        prefs.protectionEndMillis = if (afterMinutes == PrefsManager.AFTER_TIMER_UNTIL_FP) {
            -1L
        } else {
            System.currentTimeMillis() + (afterMinutes * 60_000L)
        }

        // STEP 1: Immediately pause & silence YouTube BEFORE exiting (prevents PiP minimize & background audio!)
        MediaControlManager.silenceAndStopMedia(this)

        // STEP 2: Immediately display 100% solid opaque Fake PIN Lock Overlay
        LockOverlayManager.showFakePinOverlay(this)

        // STEP 3: Play Parent's Custom Alert Sound (uses USAGE_ALARM, unaffected by media mute)
        SoundAlertManager.playAlertSound(this)

        // STEP 4: Wait 250ms so YouTube processes PAUSE, then exit cleanly to Home
        handler.postDelayed({
            val accessibility = KidShieldAccessibilityService.instance
            if (accessibility != null) {
                accessibility.performHomeAction()
            } else {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(homeIntent)
            }
        }, 250L)

        // STEP 5: Kill background processes of monitored apps & trigger Auto Force-Stop if enabled
        handler.postDelayed({
            killMonitoredBackgroundProcesses()
            val accessibility = KidShieldAccessibilityService.instance
            if (prefs.isAutoForceStopEnabled && accessibility != null) {
                accessibility.startAutoForceStop(prefs.selectedLaunchPackage)
            }
        }, 600L)

        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 1000L)
    }

    private fun killMonitoredBackgroundProcesses() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packages = prefs.getMonitoredApps().map { it.packageName }.toMutableSet()
            packages.add(prefs.selectedLaunchPackage)
            packages.forEach { pkg ->
                am.killBackgroundProcesses(pkg)
            }
        } catch (_: Exception) {
        }
    }

    private fun completeAndStopService() {
        handler.removeCallbacksAndMessages(null)
        prefs.clearAllActiveLocks()
        LockOverlayManager.removeOverlay()
        MediaControlManager.restoreMediaAfterUnlock(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundSafe(contentText: String) {
        val notification = buildNotification(contentText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, KidShieldApp.CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setContentTitle("KidShield Pro Active")
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
