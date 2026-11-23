package com.kidshield.pro.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class MonitoredApp(
    val packageName: String,
    val appName: String
)

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "kidshield_pro_prefs"
        private const val KEY_MONITORED_APPS = "monitored_apps_json"
        private const val KEY_SELECTED_PACKAGE = "selected_launch_package"
        private const val KEY_TIMER_MINUTES = "timer_minutes"
        private const val KEY_TIMER_SECONDS = "timer_seconds"
        private const val KEY_AFTER_TIMER_MINUTES = "after_timer_protection_minutes"
        private const val KEY_SOUND_URI = "custom_sound_uri"
        private const val KEY_SOUND_NAME = "custom_sound_name"

        // Runtime protection state
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_TIMER_END_MS = "timer_end_ms"
        private const val KEY_LOCK_ENFORCED = "lock_enforced"
        private const val KEY_PROTECTION_END_MS = "protection_end_ms" // -1L means Until Parent Fingerprint
        private const val KEY_ANTI_UNINSTALL_GUARD = "anti_uninstall_guard"
        private const val KEY_AUTO_FORCE_STOP = "auto_force_stop_enabled"
        private const val KEY_SAVED_MEDIA_VOLUME = "saved_media_volume"

        const val AFTER_TIMER_UNTIL_FP = -1
    }

    fun getMonitoredApps(): List<MonitoredApp> {
        val raw = prefs.getString(KEY_MONITORED_APPS, null)
        if (raw.isNullOrBlank()) {
            val defaultList = listOf(
                MonitoredApp("com.google.android.youtube", "YouTube")
            )
            saveMonitoredApps(defaultList)
            return defaultList
        }
        val list = mutableListOf<MonitoredApp>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MonitoredApp(
                        packageName = obj.getString("pkg"),
                        appName = obj.getString("name")
                    )
                )
            }
        } catch (_: Exception) {
            list.add(MonitoredApp("com.google.android.youtube", "YouTube"))
        }
        return list
    }

    fun saveMonitoredApps(apps: List<MonitoredApp>) {
        val array = JSONArray()
        apps.forEach { app ->
            val obj = JSONObject()
            obj.put("pkg", app.packageName)
            obj.put("name", app.appName)
            array.put(obj)
        }
        prefs.edit().putString(KEY_MONITORED_APPS, array.toString()).apply()
    }

    fun addMonitoredApp(app: MonitoredApp): List<MonitoredApp> {
        val current = getMonitoredApps().toMutableList()
        if (current.none { it.packageName == app.packageName }) {
            current.add(app)
            saveMonitoredApps(current)
        }
        return current
    }

    fun removeMonitoredApp(packageName: String): List<MonitoredApp> {
        val current = getMonitoredApps().filterNot { it.packageName == packageName }
        saveMonitoredApps(current)
        if (selectedLaunchPackage == packageName) {
            selectedLaunchPackage = current.firstOrNull()?.packageName ?: "com.google.android.youtube"
        }
        return current
    }

    var selectedLaunchPackage: String
        get() = prefs.getString(KEY_SELECTED_PACKAGE, "com.google.android.youtube")
            ?: "com.google.android.youtube"
        set(value) = prefs.edit().putString(KEY_SELECTED_PACKAGE, value).apply()

    var timerMinutes: Int
        get() = prefs.getInt(KEY_TIMER_MINUTES, 15)
        set(value) = prefs.edit().putInt(KEY_TIMER_MINUTES, value).apply()

    var timerSeconds: Int
        get() = prefs.getInt(KEY_TIMER_SECONDS, 0)
        set(value) = prefs.edit().putInt(KEY_TIMER_SECONDS, value).apply()

    var afterTimerProtectionMinutes: Int
        get() = prefs.getInt(KEY_AFTER_TIMER_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_AFTER_TIMER_MINUTES, value).apply()

    var customSoundUri: String?
        get() = prefs.getString(KEY_SOUND_URI, null)
        set(value) = prefs.edit().putString(KEY_SOUND_URI, value).apply()

    var customSoundName: String
        get() = prefs.getString(KEY_SOUND_NAME, "Default Alarm / डिफ़ॉल्ट साउंड")
            ?: "Default Alarm / डिफ़ॉल्ट साउंड"
        set(value) = prefs.edit().putString(KEY_SOUND_NAME, value).apply()

    var isAntiUninstallGuardEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANTI_UNINSTALL_GUARD, true)
        set(value) = prefs.edit().putBoolean(KEY_ANTI_UNINSTALL_GUARD, value).apply()

    var isAutoForceStopEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FORCE_STOP, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_FORCE_STOP, value).apply()

    var savedMediaVolume: Int
        get() = prefs.getInt(KEY_SAVED_MEDIA_VOLUME, -1)
        set(value) = prefs.edit().putInt(KEY_SAVED_MEDIA_VOLUME, value).apply()

    var isSessionActive: Boolean
        get() = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_SESSION_ACTIVE, value).apply()

    var timerEndMillis: Long
        get() = prefs.getLong(KEY_TIMER_END_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_TIMER_END_MS, value).apply()

    var isLockEnforced: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENFORCED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENFORCED, value).apply()

    var protectionEndMillis: Long
        get() = prefs.getLong(KEY_PROTECTION_END_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_PROTECTION_END_MS, value).apply()

    fun isProtectionCurrentlyActive(): Boolean {
        if (!isLockEnforced) return false
        val endMs = protectionEndMillis
        if (endMs == -1L) return true
        return System.currentTimeMillis() < endMs
    }

    fun clearAllActiveLocks() {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .putBoolean(KEY_LOCK_ENFORCED, false)
            .putLong(KEY_TIMER_END_MS, 0L)
            .putLong(KEY_PROTECTION_END_MS, 0L)
            .apply()
    }
}
