package com.kidshield.pro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kidshield.pro.data.PrefsManager

class BootAndScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_USER_PRESENT) {
            val prefs = PrefsManager(context)
            if (prefs.isProtectionCurrentlyActive()) {
                val serviceIntent = Intent(context, KidShieldTimerService::class.java).apply {
                    this.action = KidShieldTimerService.ACTION_RESUME_PROTECTION
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
