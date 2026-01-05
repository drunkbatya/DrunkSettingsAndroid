package com.drunkbatya.drunksettings.xposed

import android.os.FileObserver
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream

class SettingsFilePrefs(
    private val modulePackage: String,
    private val prefsName: String,
    private val logger: (String) -> Unit,
) {
    @Volatile
    private var values: Map<String, Any> = emptyMap()

    @Volatile
    private var prefsFile: File? = null

    private var observer: FileObserver? = null

    fun isReady(): Boolean = prefsFile != null

    fun initFromDataDirs(dataDirs: Iterable<String>) {
        if (prefsFile != null) {
            return
        }
        var selected: File? = null
        for (dir in dataDirs) {
            val candidate = File(dir, "shared_prefs/$prefsName.xml")
            if (candidate.exists()) {
                selected = candidate
                break
            }
            if (selected == null) {
                selected = candidate
            }
        }
        val file = selected ?: return
        prefsFile = file
        logger("DrunkSettings: prefs file=${file.path} exists=${file.exists()} canRead=${file.canRead()}")
        reload()
        startWatching(file)
    }

    fun reload() {
        val file = prefsFile ?: return
        values = readPrefs(file)
    }

    fun contains(key: String): Boolean = values.containsKey(key)

    fun getInt(key: String, defaultValue: Int): Int {
        val value = values[key]
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is String -> value.toIntOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    private fun startWatching(file: File) {
        if (observer != null) {
            return
        }
        val parent = file.parentFile ?: return
        val targetName = file.name
        observer = object : FileObserver(
            parent,
            CLOSE_WRITE or MOVED_TO or CREATE or DELETE or DELETE_SELF
        ) {
            override fun onEvent(event: Int, path: String?) {
                if (path != targetName) {
                    return
                }
                reload()
                logger("DrunkSettings: prefs reloaded via observer")
            }
        }.also { it.startWatching() }
    }

    private fun readPrefs(file: File): Map<String, Any> {
        if (!file.exists() || !file.canRead()) {
            return emptyMap()
        }
        val result = HashMap<String, Any>()
        try {
            FileInputStream(file).use { input ->
                val parser = Xml.newPullParser()
                parser.setInput(input, "utf-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val tag = parser.name
                        val name = parser.getAttributeValue(null, "name")
                        when (tag) {
                            "int" -> {
                                val value = parser.getAttributeValue(null, "value")?.toIntOrNull()
                                if (name != null && value != null) {
                                    result[name] = value
                                }
                            }
                            "long" -> {
                                val value = parser.getAttributeValue(null, "value")?.toLongOrNull()
                                if (name != null && value != null) {
                                    result[name] = value
                                }
                            }
                            "float" -> {
                                val value = parser.getAttributeValue(null, "value")?.toFloatOrNull()
                                if (name != null && value != null) {
                                    result[name] = value
                                }
                            }
                            "boolean" -> {
                                val value = parser.getAttributeValue(null, "value")?.toBooleanStrictOrNull()
                                if (name != null && value != null) {
                                    result[name] = value
                                }
                            }
                            "string" -> {
                                val value = parser.nextText()
                                if (name != null) {
                                    result[name] = value
                                }
                            }
                            "set" -> {
                                if (name != null) {
                                    val set = LinkedHashSet<String>()
                                    var inner = parser.next()
                                    while (!(inner == XmlPullParser.END_TAG && parser.name == "set")) {
                                        if (inner == XmlPullParser.START_TAG && parser.name == "string") {
                                            set.add(parser.nextText())
                                        }
                                        inner = parser.next()
                                    }
                                    result[name] = set
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (t: Throwable) {
            logger("DrunkSettings: failed to read prefs: ${t.javaClass.simpleName}")
            return emptyMap()
        }
        return result
    }
}
