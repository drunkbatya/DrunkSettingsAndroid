package com.drunkbatya.drunksettings.xposed.preferences

import android.content.SharedPreferences
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
        const val APP_KEY_PREFIX = "min_notification_sound_timeout_"
    }

    @Volatile
    private var generalMinSoundTimeout = 0
    @Volatile
    private var doNotFadeOutMusic = false
    @Volatile
    private var alsoMuteBlink = false
    @Volatile
    private var verboseLogging = false
    private val appTimeouts = ConcurrentHashMap<String, Int>()

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

    fun syncAll(prefs: SharedPreferences) {
        ModuleBridge.moduleInstance?.log(TAG + "syncing all settings")
        generalMinSoundTimeout = prefs.getInt(KEY_GENERAL_MIN_SOUND, 0)
        doNotFadeOutMusic = prefs.getBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, false)
        alsoMuteBlink = prefs.getBoolean(KEY_DEBUG_ALSO_MUTE_BLINK, false)
        verboseLogging = prefs.getBoolean(KEY_DEBUG_VERBOSE_LOGGING, false)
        appTimeouts.clear()
        for ((key, value) in prefs.all) {
            if (key.startsWith(APP_KEY_PREFIX) && value is Int) {
                val packageName = key.removePrefix(APP_KEY_PREFIX)
                appTimeouts[packageName] = value
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
        }
    }
}
