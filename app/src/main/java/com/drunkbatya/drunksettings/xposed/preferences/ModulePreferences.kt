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
        const val APP_KEY_PREFIX = "min_notification_sound_timeout_"
    }

    @Volatile
    private var generalMinSoundTimeout = 0
    @Volatile
    private var doNotFadeOutMusic = false
    private val appTimeouts = ConcurrentHashMap<String, Int>()

    fun resolveLimitSeconds(packageName: String): Int {
        return appTimeouts[packageName] ?: generalMinSoundTimeout
    }

    fun shouldPreventFadeOutSound(): Boolean {
        return doNotFadeOutMusic
    }

    fun syncAll(prefs: SharedPreferences) {
        ModuleBridge.moduleInstance?.log(TAG + "syncing all settings")
        generalMinSoundTimeout = prefs.getInt(KEY_GENERAL_MIN_SOUND, 0)
        doNotFadeOutMusic = prefs.getBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, false)
        appTimeouts.clear()
        for ((key, value) in prefs.all) {
            if (key.startsWith(APP_KEY_PREFIX) && value is Int) {
                val packageName = key.removePrefix(APP_KEY_PREFIX)
                appTimeouts[packageName] = value
            }
        }
    }

    fun onPreferenceChanged(prefs: SharedPreferences, key: String?) {
        ModuleBridge.moduleInstance?.log(TAG + "settings changed in UI, key: $key")
        if (key == null) {
            syncAll(prefs)
            return
        }
        when {
            key == KEY_GENERAL_MIN_SOUND -> {
                generalMinSoundTimeout = prefs.getInt(KEY_GENERAL_MIN_SOUND, 0)
            }
            key == KEY_GENERAL_DO_NOT_FADE_OUT -> {
                doNotFadeOutMusic = prefs.getBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, false)
            }
            key.startsWith(APP_KEY_PREFIX) -> {
                val packageName = key.removePrefix(APP_KEY_PREFIX)
                if (prefs.contains(key)) {
                    appTimeouts[packageName] = prefs.getInt(key, generalMinSoundTimeout)
                } else {
                    appTimeouts.remove(packageName)
                }
            }
        }
    }
}