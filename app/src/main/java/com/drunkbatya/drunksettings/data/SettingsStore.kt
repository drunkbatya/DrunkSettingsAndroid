package com.drunkbatya.drunksettings.data

import android.util.Log
import com.drunkbatya.drunksettings.ui.model.NotifDetectMode
import io.github.libxposed.service.XposedService
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

class SettingsStore(private val service: XposedService) {
    val prefs = service.getRemotePreferences(PREFS_NAME)

    companion object {
        const val PREFS_NAME = "NotificationManagerSupervisor"
        const val KEY_GENERAL_MIN_SOUND = "general_min_notification_sound_timeout"
        const val KEY_GENERAL_DO_NOT_FADE_OUT = "general_do_not_fade_out_music"
        const val KEY_DEBUG_ALSO_MUTE_BLINK = "debug_also_mute_blink"
        const val KEY_DEBUG_VERBOSE_LOGGING = "debug_verbose_logging"
        const val KEY_SWALLOW_HEADSET_BUTTON = "swallow_headset_button"
        const val KEY_NOTIF_DETECT_MODE = "notif_detect_mode"
        const val KEY_SCREENSHOT_BLOCK = "screenshot_block"
        const val KEY_CAPTURE_SECURE_LAYERS = "capture_secure_layers"
        const val NOTIF_DETECT_APP_PREFIX = "notif_detect_mode_"
        const val SCREENSHOT_BLOCK_APP_PREFIX = "screenshot_block_"
        const val DEFAULT_MIN_SOUND = 0

        fun appKey(packageName: String): String {
            return "min_notification_sound_timeout_$packageName"
        }

        fun notifDetectAppKey(packageName: String): String {
            return "$NOTIF_DETECT_APP_PREFIX$packageName"
        }

        fun screenshotBlockAppKey(packageName: String): String {
            return "$SCREENSHOT_BLOCK_APP_PREFIX$packageName"
        }
    }

    fun getAppMinSoundTimeout(packageName: String): Int? {
        val key = appKey(packageName)
        return if (prefs.contains(key)) prefs.getInt(key, DEFAULT_MIN_SOUND) else null
    }

    fun getGeneralMinSoundTimeout(): Int {
        return prefs.getInt(KEY_GENERAL_MIN_SOUND, DEFAULT_MIN_SOUND)
    }

    fun getDoNotFadeOutMusic(): Boolean {
        return prefs.getBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, false)
    }

    fun getDebugAlsoMuteBlink(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_ALSO_MUTE_BLINK, false)
    }

    fun getDebugVerboseLogging(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_VERBOSE_LOGGING, false)
    }

    fun setGeneralMinSoundTimeout(seconds: Int) {
        prefs.edit(commit = true) { putInt(KEY_GENERAL_MIN_SOUND, seconds) }
    }

    fun setDoNotFadeOutMusic(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, enabled) }
    }

    fun setDebugAlsoMuteBlink(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_DEBUG_ALSO_MUTE_BLINK, enabled) }
    }

    fun setDebugVerboseLogging(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_DEBUG_VERBOSE_LOGGING, enabled) }
    }

    fun setAppMinSoundTimeout(packageName: String, seconds: Int) {
        prefs.edit(commit = true) { putInt(appKey(packageName), seconds) }
    }

    fun clearAppMinSoundTimeout(packageName: String) {
        prefs.edit(commit = true) { remove(appKey(packageName)) }
    }

    // --- Wired headset button ---

    fun getSwallowHeadsetButton(): Boolean {
        return prefs.getBoolean(KEY_SWALLOW_HEADSET_BUTTON, false)
    }

    fun setSwallowHeadsetButton(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_SWALLOW_HEADSET_BUTTON, enabled) }
    }

    // --- Notification-status visibility ---

    fun getGeneralNotifDetectMode(): NotifDetectMode {
        return NotifDetectMode.fromStorage(prefs.getString(KEY_NOTIF_DETECT_MODE, null))
    }

    fun setGeneralNotifDetectMode(mode: NotifDetectMode) {
        prefs.edit(commit = true) { putString(KEY_NOTIF_DETECT_MODE, mode.storageValue) }
    }

    /** Per-app override, or null when the app inherits the global mode. */
    fun getAppNotifDetectMode(packageName: String): NotifDetectMode? {
        val key = notifDetectAppKey(packageName)
        return if (prefs.contains(key)) {
            NotifDetectMode.fromStorage(prefs.getString(key, null))
        } else {
            null
        }
    }

    fun setAppNotifDetectMode(packageName: String, mode: NotifDetectMode) {
        prefs.edit(commit = true) { putString(notifDetectAppKey(packageName), mode.storageValue) }
    }

    fun clearAppNotifDetectMode(packageName: String) {
        prefs.edit(commit = true) { remove(notifDetectAppKey(packageName)) }
    }

    // --- Screenshot detection ---

    fun getGeneralScreenshotBlock(): Boolean {
        return prefs.getBoolean(KEY_SCREENSHOT_BLOCK, false)
    }

    fun setGeneralScreenshotBlock(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_SCREENSHOT_BLOCK, enabled) }
    }

    /** Per-app override, or null when the app inherits the global setting. */
    fun getAppScreenshotBlock(packageName: String): Boolean? {
        val key = screenshotBlockAppKey(packageName)
        return if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    }

    fun setAppScreenshotBlock(packageName: String, enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(screenshotBlockAppKey(packageName), enabled) }
    }

    fun clearAppScreenshotBlock(packageName: String) {
        prefs.edit(commit = true) { remove(screenshotBlockAppKey(packageName)) }
    }

    // --- Force-allow screenshots (disable FLAG_SECURE) ---

    fun getCaptureSecureLayers(): Boolean {
        return prefs.getBoolean(KEY_CAPTURE_SECURE_LAYERS, false)
    }

    fun setCaptureSecureLayers(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_CAPTURE_SECURE_LAYERS, enabled) }
    }

    fun wipeAll() {
        service.deleteRemotePreferences(PREFS_NAME)
    }

    fun dumpToLog() {
        val json = JSONObject()
        val entries = prefs.all.toSortedMap()
        for ((key, value) in entries) {
            when (value) {
                null -> json.put(key, JSONObject.NULL)
                is Boolean, is Int, is Long, is Float, is String -> json.put(key, value)
                is Set<*> -> {
                    val array = JSONArray()
                    value.filterIsInstance<String>().forEach { array.put(it) }
                    json.put(key, array)
                }
                else -> json.put(key, value.toString())
            }
        }
        Log.i("DrunkSettings", "settings=$json")
    }
}
