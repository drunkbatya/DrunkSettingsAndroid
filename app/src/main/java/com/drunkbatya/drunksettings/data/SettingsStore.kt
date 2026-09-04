package com.drunkbatya.drunksettings.data

import android.util.Log
import com.drunkbatya.drunksettings.ui.model.NotifDetectMode
import io.github.libxposed.service.XposedService
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

class SettingsStore(private val service: XposedService) {
    val prefs = service.getRemotePreferences(SettingsKeys.PREFS_NAME)

    fun getAppMinSoundTimeout(packageName: String): Int? {
        val key = SettingsKeys.appMinSoundKey(packageName)
        return if (prefs.contains(key)) prefs.getInt(key, SettingsKeys.DEFAULT_MIN_SOUND) else null
    }

    fun getGeneralMinSoundTimeout(): Int {
        return prefs.getInt(SettingsKeys.GENERAL_MIN_SOUND, SettingsKeys.DEFAULT_MIN_SOUND)
    }

    fun getDoNotFadeOutMusic(): Boolean {
        return prefs.getBoolean(SettingsKeys.GENERAL_DO_NOT_FADE_OUT, false)
    }

    fun getDebugAlsoMuteBlink(): Boolean {
        return prefs.getBoolean(SettingsKeys.DEBUG_ALSO_MUTE_BLINK, false)
    }

    fun getDebugVerboseLogging(): Boolean {
        return prefs.getBoolean(SettingsKeys.DEBUG_VERBOSE_LOGGING, false)
    }

    fun setGeneralMinSoundTimeout(seconds: Int) {
        prefs.edit(commit = true) { putInt(SettingsKeys.GENERAL_MIN_SOUND, seconds) }
    }

    fun setDoNotFadeOutMusic(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.GENERAL_DO_NOT_FADE_OUT, enabled) }
    }

    fun setDebugAlsoMuteBlink(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.DEBUG_ALSO_MUTE_BLINK, enabled) }
    }

    fun setDebugVerboseLogging(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.DEBUG_VERBOSE_LOGGING, enabled) }
    }

    fun setAppMinSoundTimeout(packageName: String, seconds: Int) {
        prefs.edit(commit = true) { putInt(SettingsKeys.appMinSoundKey(packageName), seconds) }
    }

    fun clearAppMinSoundTimeout(packageName: String) {
        prefs.edit(commit = true) { remove(SettingsKeys.appMinSoundKey(packageName)) }
    }

    // --- Wired headset button ---

    fun getSwallowHeadsetButton(): Boolean {
        return prefs.getBoolean(SettingsKeys.SWALLOW_HEADSET_BUTTON, false)
    }

    fun setSwallowHeadsetButton(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.SWALLOW_HEADSET_BUTTON, enabled) }
    }

    // --- Notification-status visibility ---

    fun getGeneralNotifDetectMode(): NotifDetectMode {
        return NotifDetectMode.fromStorage(prefs.getString(SettingsKeys.NOTIF_DETECT_MODE, null))
    }

    fun setGeneralNotifDetectMode(mode: NotifDetectMode) {
        prefs.edit(commit = true) { putString(SettingsKeys.NOTIF_DETECT_MODE, mode.storageValue) }
    }

    /** Per-app override, or null when the app inherits the global mode. */
    fun getAppNotifDetectMode(packageName: String): NotifDetectMode? {
        val key = SettingsKeys.notifDetectAppKey(packageName)
        return if (prefs.contains(key)) {
            NotifDetectMode.fromStorage(prefs.getString(key, null))
        } else {
            null
        }
    }

    fun setAppNotifDetectMode(packageName: String, mode: NotifDetectMode) {
        prefs.edit(commit = true) { putString(SettingsKeys.notifDetectAppKey(packageName), mode.storageValue) }
    }

    fun clearAppNotifDetectMode(packageName: String) {
        prefs.edit(commit = true) { remove(SettingsKeys.notifDetectAppKey(packageName)) }
    }

    // --- Screenshot detection ---

    fun getGeneralScreenshotBlock(): Boolean {
        return prefs.getBoolean(SettingsKeys.SCREENSHOT_BLOCK, false)
    }

    fun setGeneralScreenshotBlock(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.SCREENSHOT_BLOCK, enabled) }
    }

    /** Per-app override, or null when the app inherits the global setting. */
    fun getAppScreenshotBlock(packageName: String): Boolean? {
        val key = SettingsKeys.screenshotBlockAppKey(packageName)
        return if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    }

    fun setAppScreenshotBlock(packageName: String, enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.screenshotBlockAppKey(packageName), enabled) }
    }

    fun clearAppScreenshotBlock(packageName: String) {
        prefs.edit(commit = true) { remove(SettingsKeys.screenshotBlockAppKey(packageName)) }
    }

    // --- Force-allow screenshots (disable FLAG_SECURE) ---

    fun getCaptureSecureLayers(): Boolean {
        return prefs.getBoolean(SettingsKeys.CAPTURE_SECURE_LAYERS, false)
    }

    fun setCaptureSecureLayers(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.CAPTURE_SECURE_LAYERS, enabled) }
    }

    // --- Fingerprint gating ---

    fun getFingerprintScreenOnOnly(): Boolean {
        return prefs.getBoolean(SettingsKeys.FINGERPRINT_SCREEN_ON_ONLY, false)
    }

    fun setFingerprintScreenOnOnly(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(SettingsKeys.FINGERPRINT_SCREEN_ON_ONLY, enabled) }
    }

    fun wipeAll() {
        service.deleteRemotePreferences(SettingsKeys.PREFS_NAME)
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
