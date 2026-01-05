package com.drunkbatya.drunksettings.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SettingsStore {
    const val PREFS_NAME = "settings"
    const val KEY_GENERAL_MIN_SOUND = "notifications.general.min_notification_sound_timeout"
    const val DEFAULT_MIN_SOUND = 5

    fun appKey(packageName: String): String {
        return "notifications.app_notifications.$packageName.min_notification_sound_timeout"
    }

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGeneralMinSoundTimeout(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_GENERAL_MIN_SOUND, DEFAULT_MIN_SOUND)
    }

    fun getAppMinSoundTimeout(context: Context, packageName: String): Int? {
        val prefs = getPrefs(context)
        val key = appKey(packageName)
        return if (prefs.contains(key)) prefs.getInt(key, DEFAULT_MIN_SOUND) else null
    }

    fun getAppOrGeneralTimeout(context: Context, packageName: String): Int {
        return getAppMinSoundTimeout(context, packageName) ?: getGeneralMinSoundTimeout(context)
    }

    fun setGeneralMinSoundTimeout(context: Context, seconds: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(KEY_GENERAL_MIN_SOUND, seconds).commit()
        makePrefsReadable(context)
    }

    fun setAppMinSoundTimeout(context: Context, packageName: String, seconds: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(appKey(packageName), seconds).commit()
        makePrefsReadable(context)
    }

    fun clearAppMinSoundTimeout(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        prefs.edit().remove(appKey(packageName)).commit()
        makePrefsReadable(context)
    }

    fun wipeAll(context: Context) {
        val deleted = context.deleteSharedPreferences(PREFS_NAME)
        if (!deleted) {
            val prefs = getPrefs(context)
            prefs.edit().clear().commit()
            val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$PREFS_NAME.xml")
            prefsFile.delete()
        }
    }

    fun dumpToLog(context: Context) {
        val prefs = getPrefs(context)
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

    private fun makePrefsReadable(context: Context) {
        val dataDir = context.applicationInfo.dataDir
        val prefsDir = File(dataDir, "shared_prefs")
        val appDir = File(dataDir)
        appDir.setExecutable(true, false)
        prefsDir.setReadable(true, false)
        prefsDir.setExecutable(true, false)
        val prefsFile = File(prefsDir, "$PREFS_NAME.xml")
        prefsFile.setReadable(true, false)
    }

}
