package com.drunkbatya.drunksettings.xposed.preferences

import android.content.SharedPreferences
import com.drunkbatya.drunksettings.ui.model.NotifDetectMode
import com.drunkbatya.drunksettings.xposed.ModuleBridge
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator

class ModulePreferences {
    companion object {
        private const val TAG = "ModulePreferences: "
        const val PREFS_NAME = "NotificationManagerSupervisor"
        const val KEY_GENERAL_MIN_SOUND = "general_min_notification_sound_timeout"
        const val KEY_GENERAL_DO_NOT_FADE_OUT = "general_do_not_fade_out_music"
        const val KEY_DEBUG_ALSO_MUTE_BLINK = "debug_also_mute_blink"
        const val KEY_DEBUG_VERBOSE_LOGGING = "debug_verbose_logging"
        const val KEY_SWALLOW_HEADSET_BUTTON = "swallow_headset_button"
        const val KEY_NOTIF_DETECT_MODE = "notif_detect_mode"
        const val KEY_SCREENSHOT_BLOCK = "screenshot_block"
        const val KEY_CAPTURE_SECURE_LAYERS = "capture_secure_layers"
        const val APP_KEY_PREFIX = "min_notification_sound_timeout_"
        const val NOTIF_DETECT_APP_PREFIX = "notif_detect_mode_"
        const val SCREENSHOT_BLOCK_APP_PREFIX = "screenshot_block_"
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
        ModuleBridge.moduleInstance?.log(TAG + "syncing all settings")
        generalMinSoundTimeout = prefs.getInt(KEY_GENERAL_MIN_SOUND, 0)
        doNotFadeOutMusic = prefs.getBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, false)
        alsoMuteBlink = prefs.getBoolean(KEY_DEBUG_ALSO_MUTE_BLINK, false)
        verboseLogging = prefs.getBoolean(KEY_DEBUG_VERBOSE_LOGGING, false)
        swallowHeadsetButton = prefs.getBoolean(KEY_SWALLOW_HEADSET_BUTTON, false)
        notifDetectGlobal = NotifDetectMode.fromStorage(prefs.getString(KEY_NOTIF_DETECT_MODE, null))
        screenshotBlockGlobal = prefs.getBoolean(KEY_SCREENSHOT_BLOCK, false)
        captureSecureLayers = prefs.getBoolean(KEY_CAPTURE_SECURE_LAYERS, false)
        appTimeouts.clear()
        notifDetectByApp.clear()
        screenshotBlockByApp.clear()
        for ((key, value) in prefs.all) {
            when {
                key.startsWith(APP_KEY_PREFIX) && value is Int -> {
                    appTimeouts[key.removePrefix(APP_KEY_PREFIX)] = value
                }
                key.startsWith(NOTIF_DETECT_APP_PREFIX) && value is String -> {
                    notifDetectByApp[key.removePrefix(NOTIF_DETECT_APP_PREFIX)] =
                        NotifDetectMode.fromStorage(value)
                }
                key.startsWith(SCREENSHOT_BLOCK_APP_PREFIX) && value is Boolean -> {
                    screenshotBlockByApp[key.removePrefix(SCREENSHOT_BLOCK_APP_PREFIX)] = value
                }
            }
        }
    }

    fun onPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == null) {
            syncAll(prefs)
            return
        }
        ModuleBridge.moduleInstance?.log(TAG + "settings changed in UI, key: $key")
        when {
            key == KEY_GENERAL_MIN_SOUND -> {
                generalMinSoundTimeout = prefs.getInt(KEY_GENERAL_MIN_SOUND, 0)
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $generalMinSoundTimeout")
            }
            key == KEY_GENERAL_DO_NOT_FADE_OUT -> {
                doNotFadeOutMusic = prefs.getBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, false)
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $doNotFadeOutMusic")
            }
            key == KEY_DEBUG_ALSO_MUTE_BLINK -> {
                alsoMuteBlink = prefs.getBoolean(KEY_DEBUG_ALSO_MUTE_BLINK, false)
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $alsoMuteBlink")
            }
            key == KEY_DEBUG_VERBOSE_LOGGING -> {
                verboseLogging = prefs.getBoolean(KEY_DEBUG_VERBOSE_LOGGING, false)
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $verboseLogging")
            }
            key == KEY_SWALLOW_HEADSET_BUTTON -> {
                swallowHeadsetButton = prefs.getBoolean(KEY_SWALLOW_HEADSET_BUTTON, false)
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $swallowHeadsetButton")
            }
            key == KEY_NOTIF_DETECT_MODE -> {
                notifDetectGlobal = NotifDetectMode.fromStorage(prefs.getString(KEY_NOTIF_DETECT_MODE, null))
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $notifDetectGlobal")
            }
            key == KEY_SCREENSHOT_BLOCK -> {
                screenshotBlockGlobal = prefs.getBoolean(KEY_SCREENSHOT_BLOCK, false)
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $screenshotBlockGlobal")
            }
            key == KEY_CAPTURE_SECURE_LAYERS -> {
                captureSecureLayers = prefs.getBoolean(KEY_CAPTURE_SECURE_LAYERS, false)
                ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: $captureSecureLayers")
            }
            key.startsWith(APP_KEY_PREFIX) -> {
                val packageName = key.removePrefix(APP_KEY_PREFIX)
                if (prefs.contains(key)) {
                    appTimeouts[packageName] = prefs.getInt(key, generalMinSoundTimeout)
                    ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: ${appTimeouts[packageName]}")
                } else {
                    appTimeouts.remove(packageName)
                    ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: removed")
                }
            }
            key.startsWith(NOTIF_DETECT_APP_PREFIX) -> {
                val packageName = key.removePrefix(NOTIF_DETECT_APP_PREFIX)
                if (prefs.contains(key)) {
                    notifDetectByApp[packageName] = NotifDetectMode.fromStorage(prefs.getString(key, null))
                    ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: ${notifDetectByApp[packageName]}")
                } else {
                    notifDetectByApp.remove(packageName)
                    ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: removed")
                }
            }
            key.startsWith(SCREENSHOT_BLOCK_APP_PREFIX) -> {
                val packageName = key.removePrefix(SCREENSHOT_BLOCK_APP_PREFIX)
                if (prefs.contains(key)) {
                    screenshotBlockByApp[packageName] = prefs.getBoolean(key, false)
                    ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: ${screenshotBlockByApp[packageName]}")
                } else {
                    screenshotBlockByApp.remove(packageName)
                    ModuleBridge.moduleInstance?.logVerbose(TAG + "key: $key, new value: removed")
                }
            }
        }
    }
}
