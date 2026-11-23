package com.kidshield.pro.security

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import com.kidshield.pro.data.PrefsManager

object MediaControlManager {

    private var activeFocusRequest: AudioFocusRequest? = null
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener {
        // Hold focus silently while lock screen is active
    }

    /**
     * Immediately pauses YouTube / any active media player, claims exclusive audio focus
     * (preventing Picture-in-Picture and background audio), and mutes STREAM_MUSIC
     * while preserving STREAM_ALARM for Parent's custom alert sound.
     */
    fun silenceAndStopMedia(context: Context) {
        val appCtx = context.applicationContext
        val audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val prefs = PrefsManager(appCtx)

        // 1. Dispatch hardware MEDIA_PAUSE and MEDIA_STOP key events to pause YouTube immediately
        sendMediaKey(audioManager, KeyEvent.KEYCODE_MEDIA_PAUSE)
        sendMediaKey(audioManager, KeyEvent.KEYCODE_MEDIA_STOP)

        // 2. Request exclusive permanent AUDIOFOCUS_GAIN so YouTube halts playback right away
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                activeFocusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (_: Exception) {
        }

        // 3. Save original media volume and mute STREAM_MUSIC (does NOT mute STREAM_ALARM)
        try {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (prefs.savedMediaVolume < 0 && currentVol > 0) {
                prefs.savedMediaVolume = currentVol
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {
        }
    }

    /**
     * Releases exclusive audio focus and restores original media volume when Parent unlocks.
     */
    fun restoreMediaAfterUnlock(context: Context) {
        val appCtx = context.applicationContext
        val audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val prefs = PrefsManager(appCtx)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeFocusRequest?.let { req ->
                    audioManager.abandonAudioFocusRequest(req)
                }
                activeFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusChangeListener)
            }
        } catch (_: Exception) {
        }

        try {
            val savedVol = prefs.savedMediaVolume
            if (savedVol >= 0) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    savedVol.coerceIn(0, maxVol),
                    0
                )
                prefs.savedMediaVolume = -1
            }
        } catch (_: Exception) {
        }
    }

    private fun sendMediaKey(audioManager: AudioManager, keyCode: Int) {
        try {
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(down)
            audioManager.dispatchMediaKeyEvent(up)
        } catch (_: Exception) {
        }
    }
}
