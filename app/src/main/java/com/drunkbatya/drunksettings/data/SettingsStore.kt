package com.drunkbatya.drunksettings.data

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.service.XposedService
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(private val service: XposedService) {
    companion object {
        const val PREFS_NAME = "NotificationManagerSupervisor"
        const val KEY_GENERAL_MIN_SOUND = "general_min_notification_sound_timeout"
        const val DEFAULT_MIN_SOUND = 0

        fun appKey(packageName: String): String {
            return "min_notification_sound_timeout_$packageName"
        }
    }

    private val prefsCache: SharedPreferences by lazy {
        service.getRemotePreferences(PREFS_NAME)
    }

    fun getPrefs(): SharedPreferences {
        return prefsCache
    }

    fun getAppMinSoundTimeout(packageName: String): Int? {
        val key = appKey(packageName)
        return if (prefsCache.contains(key)) prefsCache.getInt(key, DEFAULT_MIN_SOUND) else null
    }

    fun setGeneralMinSoundTimeout(seconds: Int) {
        prefsCache.edit().putInt(KEY_GENERAL_MIN_SOUND, seconds).commit()
    }

    fun setAppMinSoundTimeout(packageName: String, seconds: Int) {
        prefsCache.edit().putInt(appKey(packageName), seconds).commit()
    }

    fun clearAppMinSoundTimeout(packageName: String) {
        prefsCache.edit().remove(appKey(packageName)).commit()
    }

    fun wipeAll() {
        service.deleteRemotePreferences(PREFS_NAME)
    }

    fun dumpToLog() {
        val json = JSONObject()
        val entries = prefsCache.all.toSortedMap()
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
