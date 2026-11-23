package com.kidshield.pro.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.kidshield.pro.R
import com.kidshield.pro.data.PrefsManager
import com.kidshield.pro.ui.BiometricUnlockActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LockOverlayManager {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var enteredPin = StringBuilder()
    private var isTemporarilyHiddenForBiometric = false

    private val clockUpdater = object : Runnable {
        override fun run() {
            updateClockAndStatus()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    fun isOverlayShowing(): Boolean = overlayView != null && overlayView?.visibility == View.VISIBLE

    fun showFakePinOverlay(context: Context) {
        mainHandler.post {
            val appCtx = context.applicationContext
            if (!Settings.canDrawOverlays(appCtx)) return@post

            if (overlayView != null) {
                if (!isTemporarilyHiddenForBiometric) {
                    overlayView?.visibility = View.VISIBLE
                }
                return@post
            }

            val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val themedCtx = ContextThemeWrapper(appCtx, R.style.Theme_KidShieldPro)
            val view = LayoutInflater.from(themedCtx).inflate(R.layout.overlay_fake_pin_lock, null)

            @Suppress("DEPRECATION")
            val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.OPAQUE
            ).apply {
                gravity = Gravity.CENTER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            @Suppress("DEPRECATION")
            view.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

            setupKeypadLogic(appCtx, view)
            overlayView = view
            isTemporarilyHiddenForBiometric = false

            try {
                wm.addView(view, params)
                mainHandler.removeCallbacks(clockUpdater)
                mainHandler.post(clockUpdater)
            } catch (_: Exception) {
                overlayView = null
            }
        }
    }

    fun hideTemporarilyForBiometric() {
        mainHandler.post {
            isTemporarilyHiddenForBiometric = true
            overlayView?.visibility = View.GONE
        }
    }

    fun restoreAfterBiometricCancel(context: Context) {
        mainHandler.post {
            isTemporarilyHiddenForBiometric = false
            val prefs = PrefsManager(context)
            if (prefs.isProtectionCurrentlyActive()) {
                if (overlayView != null) {
                    overlayView?.visibility = View.VISIBLE
                } else {
                    showFakePinOverlay(context)
                }
            }
        }
    }

    fun removeOverlay() {
        mainHandler.post {
            mainHandler.removeCallbacks(clockUpdater)
            isTemporarilyHiddenForBiometric = false
            enteredPin.clear()
            overlayView?.let { view ->
                try {
                    windowManager?.removeViewImmediate(view)
                } catch (_: Exception) {
                }
            }
            overlayView = null
        }
    }

    private fun setupKeypadLogic(context: Context, view: View) {
        enteredPin.clear()
        val dots = listOf(
            view.findViewById<View>(R.id.dot_1),
            view.findViewById<View>(R.id.dot_2),
            view.findViewById<View>(R.id.dot_3),
            view.findViewById<View>(R.id.dot_4)
        )
        val dotsContainer = view.findViewById<LinearLayout>(R.id.pin_dots_container)
        val errorTv = view.findViewById<TextView>(R.id.tv_pin_error)

        fun refreshDots(isError: Boolean = false) {
            dots.forEachIndexed { idx, dot ->
                val bg = when {
                    isError -> R.drawable.pin_dot_error
                    idx < enteredPin.length -> R.drawable.pin_dot_filled
                    else -> R.drawable.pin_dot_empty
                }
                dot.setBackgroundResource(bg)
            }
        }

        val digitIds = listOf(
            R.id.key_0 to "0", R.id.key_1 to "1", R.id.key_2 to "2",
            R.id.key_3 to "3", R.id.key_4 to "4", R.id.key_5 to "5",
            R.id.key_6 to "6", R.id.key_7 to "7", R.id.key_8 to "8",
            R.id.key_9 to "9"
        )

        digitIds.forEach { (id, digit) ->
            view.findViewById<TextView>(id).setOnClickListener {
                if (enteredPin.length >= 4) return@setOnClickListener
                errorTv.text = ""
                enteredPin.append(digit)
                refreshDots()

                // FAKE PIN TRAP: Any 4-digit PIN always fails!
                if (enteredPin.length == 4) {
                    refreshDots(isError = true)
                    errorTv.text = "Incorrect PIN • गलत पिन! फिर कोशिश करें"
                    vibrateError(context)

                    val shake = TranslateAnimation(-18f, 18f, 0f, 0f).apply {
                        duration = 55
                        repeatMode = TranslateAnimation.REVERSE
                        repeatCount = 5
                    }
                    dotsContainer.startAnimation(shake)

                    mainHandler.postDelayed({
                        enteredPin.clear()
                        refreshDots()
                    }, 650L)
                }
            }
        }

        view.findViewById<TextView>(R.id.key_clear).setOnClickListener {
            enteredPin.clear()
            errorTv.text = ""
            refreshDots()
        }

        view.findViewById<TextView>(R.id.key_del).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                refreshDots()
            }
        }

        // Only Parent Fingerprint can unlock!
        view.findViewById<ImageButton>(R.id.btn_parent_fingerprint).setOnClickListener {
            hideTemporarilyForBiometric()
            val intent = Intent(context, BiometricUnlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    private fun updateClockAndStatus() {
        val view = overlayView ?: return
        val timeTv = view.findViewById<TextView>(R.id.tv_lock_time)
        val statusTv = view.findViewById<TextView>(R.id.tv_protection_status)

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeTv.text = timeFmt.format(Date())

        val prefs = PrefsManager(view.context)
        val endMs = prefs.protectionEndMillis
        if (!prefs.isProtectionCurrentlyActive()) {
            removeOverlay()
            return
        }

        statusTv.text = if (endMs == -1L) {
            "LOCKED • FINGERPRINT ONLY"
        } else {
            val remainingSec = ((endMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            val mins = remainingSec / 60
            val secs = remainingSec % 60
            String.format(Locale.getDefault(), "LOCKED • %02d:%02d LEFT", mins, secs)
        }
    }

    private fun vibrateError(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) {
        }
    }
}
