package com.drunkbatya.drunksettings.data

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.service.XposedService
import org.json.JSONArray
import org.json.JSONObject

object SettingsStore {
    const val PREFS_NAME = "NotificationManagerSupervisor"
    const val KEY_GENERAL_MIN_SOUND = "general_min_notification_sound_timeout"
    const val DEFAULT_MIN_SOUND = 0

    fun appKey(packageName: String): String {
        return "min_notification_sound_timeout_$packageName"
    }

    fun getPrefs(mService: XposedService?): SharedPreferences {
        val prefs = mService?.getRemotePreferences(PREFS_NAME)
        if (prefs == null) {
            throw Exception("fuck")
        }
        return prefs
    }

    fun getAppMinSoundTimeout(mService: XposedService?, packageName: String): Int? {
        val prefs = getPrefs(mService) ?: return DEFAULT_MIN_SOUND
        val key = appKey(packageName)
        return if (prefs.contains(key)) prefs.getInt(key, DEFAULT_MIN_SOUND) else null
    }

    fun setGeneralMinSoundTimeout(mService: XposedService?, seconds: Int) {
        val prefs = getPrefs(mService) ?: return
        prefs.edit().putInt(KEY_GENERAL_MIN_SOUND, seconds).commit()
    }

    fun setAppMinSoundTimeout(mService: XposedService?, packageName: String, seconds: Int) {
        val prefs = getPrefs(mService) ?: return
        prefs.edit().putInt(appKey(packageName), seconds).commit()
    }

    fun clearAppMinSoundTimeout(mService: XposedService?, packageName: String) {
        val prefs = getPrefs(mService) ?: return
        prefs.edit().remove(appKey(packageName)).commit()
    }

    fun wipeAll(mService: XposedService?) {
        mService?.deleteRemotePreferences(PREFS_NAME)
    }

    fun dumpToLog(mService: XposedService?) {
        val prefs = getPrefs(mService) ?: return
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
