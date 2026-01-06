package com.drunkbatya.drunksettings.data

import android.util.Log
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
        const val DEFAULT_MIN_SOUND = 0

        fun appKey(packageName: String): String {
            return "min_notification_sound_timeout_$packageName"
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

    fun setGeneralMinSoundTimeout(seconds: Int) {
        prefs.edit(commit = true) { putInt(KEY_GENERAL_MIN_SOUND, seconds) }
    }

    fun setDoNotFadeOutMusic(enabled: Boolean) {
        prefs.edit(commit = true) { putBoolean(KEY_GENERAL_DO_NOT_FADE_OUT, enabled) }
    }

    fun setAppMinSoundTimeout(packageName: String, seconds: Int) {
        prefs.edit(commit = true) { putInt(appKey(packageName), seconds) }
    }

    fun clearAppMinSoundTimeout(packageName: String) {
        prefs.edit(commit = true) { remove(appKey(packageName)) }
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
