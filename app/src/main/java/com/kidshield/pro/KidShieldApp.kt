package com.kidshield.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class KidShieldApp : Application() {
    companion object {
        const val CHANNEL_TIMER_ID = "kidshield_timer_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "KidShield Pro Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active timer countdown and child lock protection status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
