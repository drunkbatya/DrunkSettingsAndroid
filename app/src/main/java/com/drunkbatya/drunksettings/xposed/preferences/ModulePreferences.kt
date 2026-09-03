package com.drunkbatya.drunksettings.xposed.preferences

import android.content.SharedPreferences
import com.drunkbatya.drunksettings.data.SettingsKeys
import com.drunkbatya.drunksettings.ui.model.NotifDetectMode
import java.util.concurrent.ConcurrentHashMap

class ModulePreferences(
    private val log: (String) -> Unit,
    private val logVerbose: (String) -> Unit,
) {
    companion object {
        private const val TAG = "ModulePreferences: "
    }

    @Volatile
    private var generalMinSoundTimeout = 0
    @Volatile
    private var doNotFadeOutMusic = false
    @Volatile
    private var alsoMuteBlink = false
    @Volatile
    private var verboseLogging = false
    @Volatile
    private var swallowHeadsetButton = false
    @Volatile
    private var notifDetectGlobal = NotifDetectMode.DEFAULT
    @Volatile
    private var screenshotBlockGlobal = false
    @Volatile
    private var captureSecureLayers = false
    private val appTimeouts = ConcurrentHashMap<String, Int>()
    private val notifDetectByApp = ConcurrentHashMap<String, NotifDetectMode>()
    private val screenshotBlockByApp = ConcurrentHashMap<String, Boolean>()

    fun resolveLimitSeconds(packageName: String): Int {
        return appTimeouts[packageName] ?: generalMinSoundTimeout
    }

    fun shouldPreventFadeOutSound(): Boolean {
        return doNotFadeOutMusic
    }

    fun shouldMuteBlinkWithSound(): Boolean {
        return alsoMuteBlink
    }

    fun isVerboseLogging(): Boolean {
        return verboseLogging
    }

    fun shouldSwallowHeadsetButton(): Boolean {
        return swallowHeadsetButton
    }

    fun resolveNotifDetectMode(packageName: String): NotifDetectMode {
        return notifDetectByApp[packageName] ?: notifDetectGlobal
    }

    fun shouldBlockScreenshotDetection(packageName: String): Boolean {
        return screenshotBlockByApp[packageName] ?: screenshotBlockGlobal
    }

    fun shouldCaptureSecureLayers(): Boolean {
        return captureSecureLayers
    }

    fun syncAll(prefs: SharedPreferences) {
        log(TAG + "syncing all settings")
        generalMinSoundTimeout = prefs.getInt(SettingsKeys.GENERAL_MIN_SOUND, 0)
        doNotFadeOutMusic = prefs.getBoolean(SettingsKeys.GENERAL_DO_NOT_FADE_OUT, false)
        alsoMuteBlink = prefs.getBoolean(SettingsKeys.DEBUG_ALSO_MUTE_BLINK, false)
        verboseLogging = prefs.getBoolean(SettingsKeys.DEBUG_VERBOSE_LOGGING, false)
        swallowHeadsetButton = prefs.getBoolean(SettingsKeys.SWALLOW_HEADSET_BUTTON, false)
        notifDetectGlobal = NotifDetectMode.fromStorage(prefs.getString(SettingsKeys.NOTIF_DETECT_MODE, null))
        screenshotBlockGlobal = prefs.getBoolean(SettingsKeys.SCREENSHOT_BLOCK, false)
        captureSecureLayers = prefs.getBoolean(SettingsKeys.CAPTURE_SECURE_LAYERS, false)
        appTimeouts.clear()
        notifDetectByApp.clear()
        screenshotBlockByApp.clear()
        for ((key, value) in prefs.all) {
            when {
                key.startsWith(SettingsKeys.APP_MIN_SOUND_PREFIX) && value is Int -> {
                    appTimeouts[key.removePrefix(SettingsKeys.APP_MIN_SOUND_PREFIX)] = value
                }
                key.startsWith(SettingsKeys.NOTIF_DETECT_APP_PREFIX) && value is String -> {
                    notifDetectByApp[key.removePrefix(SettingsKeys.NOTIF_DETECT_APP_PREFIX)] =
                        NotifDetectMode.fromStorage(value)
                }
                key.startsWith(SettingsKeys.SCREENSHOT_BLOCK_APP_PREFIX) && value is Boolean -> {
                    screenshotBlockByApp[key.removePrefix(SettingsKeys.SCREENSHOT_BLOCK_APP_PREFIX)] = value
                }
            }
        }
    }

    fun onPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == null) {
            syncAll(prefs)
            return
        }
        log(TAG + "settings changed in UI, key: $key")
        val newValue = applyChange(prefs, key) ?: return
        logVerbose(TAG + "key: $key, new value: $newValue")
    }

    private fun applyChange(prefs: SharedPreferences, key: String): Any? = when (key) {
        SettingsKeys.GENERAL_MIN_SOUND -> prefs.getInt(key, 0).also { generalMinSoundTimeout = it }
        SettingsKeys.GENERAL_DO_NOT_FADE_OUT -> prefs.getBoolean(key, false).also { doNotFadeOutMusic = it }
        SettingsKeys.DEBUG_ALSO_MUTE_BLINK -> prefs.getBoolean(key, false).also { alsoMuteBlink = it }
        SettingsKeys.DEBUG_VERBOSE_LOGGING -> prefs.getBoolean(key, false).also { verboseLogging = it }
        SettingsKeys.SWALLOW_HEADSET_BUTTON -> prefs.getBoolean(key, false).also { swallowHeadsetButton = it }
        SettingsKeys.NOTIF_DETECT_MODE ->
            NotifDetectMode.fromStorage(prefs.getString(key, null)).also { notifDetectGlobal = it }
        SettingsKeys.SCREENSHOT_BLOCK -> prefs.getBoolean(key, false).also { screenshotBlockGlobal = it }
        SettingsKeys.CAPTURE_SECURE_LAYERS -> prefs.getBoolean(key, false).also { captureSecureLayers = it }
        else -> applyAppChange(prefs, key)
    }

    private fun applyAppChange(prefs: SharedPreferences, key: String): Any? = when {
        key.startsWith(SettingsKeys.APP_MIN_SOUND_PREFIX) -> {
            val packageName = key.removePrefix(SettingsKeys.APP_MIN_SOUND_PREFIX)
            if (prefs.contains(key)) {
                prefs.getInt(key, generalMinSoundTimeout).also { appTimeouts[packageName] = it }
            } else {
                appTimeouts.remove(packageName)
                "removed"
            }
        }
        key.startsWith(SettingsKeys.NOTIF_DETECT_APP_PREFIX) -> {
            val packageName = key.removePrefix(SettingsKeys.NOTIF_DETECT_APP_PREFIX)
            if (prefs.contains(key)) {
                NotifDetectMode.fromStorage(prefs.getString(key, null)).also { notifDetectByApp[packageName] = it }
            } else {
                notifDetectByApp.remove(packageName)
                "removed"
            }
        }
        key.startsWith(SettingsKeys.SCREENSHOT_BLOCK_APP_PREFIX) -> {
            val packageName = key.removePrefix(SettingsKeys.SCREENSHOT_BLOCK_APP_PREFIX)
            if (prefs.contains(key)) {
                prefs.getBoolean(key, false).also { screenshotBlockByApp[packageName] = it }
            } else {
                screenshotBlockByApp.remove(packageName)
                "removed"
            }
        }
        else -> null
    }
}
