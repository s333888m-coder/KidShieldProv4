package com.kidshield.pro.security

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.kidshield.pro.data.PrefsManager

object SoundAlertManager {

    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun playAlertSound(context: Context, durationLimitMs: Long = 6000L) {
        stopSound()
        val prefs = PrefsManager(context)
        val uriString = prefs.customSoundUri
        val targetUri: Uri = if (!uriString.isNullOrBlank()) {
            Uri.parse(uriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context.applicationContext, targetUri)
                isLooping = false
                prepare()
                start()
            }
            mediaPlayer = player
            player.setOnCompletionListener { stopSound() }

            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.postDelayed({ stopSound() }, durationLimitMs)
        } catch (_: Exception) {
            // Fallback to default notification sound if custom file permission was revoked
            playFallbackNotification(context)
        }
    }

    private fun playFallbackNotification(context: Context) {
        try {
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, fallbackUri)
            ringtone?.play()
        } catch (_: Exception) {
            // Ignore audio playback errors
        }
    }

    fun stopSound() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }
}
