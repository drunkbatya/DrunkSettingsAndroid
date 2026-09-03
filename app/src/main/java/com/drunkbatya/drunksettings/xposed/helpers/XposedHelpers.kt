package com.drunkbatya.drunksettings.xposed.helpers

import android.app.NotificationChannel
import android.content.Context
import android.media.AudioManager
import android.os.Parcel
import android.view.KeyEvent
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method

object XposedHelpers {
    fun extractPackageName(record: Any): String? {
        val sbn = runCatching { record.javaClass.getMethod("getSbn").invoke(record) }.getOrNull()
            ?: runCatching {
                record.javaClass.getDeclaredField("sbn").apply { isAccessible = true }.get(record)
            }.getOrNull()
        return runCatching { sbn?.javaClass?.getMethod("getPackageName")?.invoke(sbn) as? String }
            .getOrNull()
    }
    fun hookTargetMethod(
        module: XposedModule,
        targetClass: Class<*>,
        targetMethod: String,
        hooker: XposedInterface.Hooker,
        log: (String) -> Unit
    ) {
        try {
            val methods = LinkedHashSet<Method>()
            targetClass.declaredMethods.forEach { methods.add(it) }
            targetClass.methods.forEach { methods.add(it) }
            var hooked = 0
            for (method in methods) {
                if (method.name != targetMethod) {
                    continue
                }
                runCatching { method.isAccessible = true }
                module.hook(method).intercept(hooker)
                hooked += 1
            }
            if (hooked == 0) {
                log("method not found: $targetMethod in ${targetClass.name}")
            } else {
                log("successfully hooked $hooked method(s): $targetMethod in ${targetClass.name}")
            }
        } catch (_: SecurityException) {
            log("security exception while hooking $targetMethod in ${targetClass.name}")
        } catch (e: Throwable) {
            log("unexpected error while hooking $targetMethod in ${targetClass.name}: ${e.message}")
        }
    }

    fun isMusicPlaying(context: Context?): Boolean {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        return audioManager.isMusicActive
    }

    fun findNotificationRecord(args: List<Any?>): Any? {
        return args.firstOrNull { arg ->
            arg != null && arg.javaClass.name.endsWith("NotificationRecord")
        }
    }

    fun findClass(
        name: String,
        classLoader: ClassLoader
    ): Class<*>? {
        return runCatching { Class.forName(name, false, classLoader) }.getOrNull()
    }

    fun findKeyEvent(args: List<Any?>): KeyEvent? {
        return args.firstOrNull { it is KeyEvent } as? KeyEvent
    }

    /**
     * Sets a (possibly final) boolean field by name, walking up the class hierarchy. ART permits
     * writing final instance fields via reflection. Returns true on success.
     */
    fun setBooleanField(instance: Any, fieldName: String, value: Boolean): Boolean {
        var cls: Class<*>? = instance.javaClass
        while (cls != null) {
            val field = runCatching { cls!!.getDeclaredField(fieldName) }.getOrNull()
            if (field != null) {
                return runCatching {
                    field.isAccessible = true
                    field.setBoolean(instance, value)
                    true
                }.getOrDefault(false)
            }
            cls = cls.superclass
        }
        return false
    }

    /** Reads a `mContext` field (present on most system_server services) reflectively. */
    fun readContextField(instance: Any?): Context? {
        if (instance == null) return null
        return runCatching {
            instance.javaClass.getDeclaredField("mContext").apply {
                isAccessible = true
            }.get(instance) as? Context
        }.getOrNull()
    }

    fun firstStringArg(args: List<Any?>): String? {
        return args.firstOrNull { it is String } as? String
    }

    /** Reads ActivityRecord.packageName (the package owning the activity) reflectively. */
    fun extractActivityPackageName(activityRecord: Any?): String? {
        if (activityRecord == null) return null
        runCatching {
            val field = activityRecord.javaClass.getDeclaredField("packageName").apply {
                isAccessible = true
            }
            (field.get(activityRecord) as? String)?.let { return it }
        }
        return runCatching {
            val component = activityRecord.javaClass.getDeclaredField("mActivityComponent").apply {
                isAccessible = true
            }.get(activityRecord)
            component?.javaClass?.getMethod("getPackageName")?.invoke(component) as? String
        }.getOrNull()
    }

    /**
     * Returns a detached copy of a NotificationChannel so callers can mutate importance without
     * touching the live instance the notification service may keep cached.
     */
    fun cloneNotificationChannel(channel: NotificationChannel): NotificationChannel? {
        val parcel = Parcel.obtain()
        return try {
            channel.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            NotificationChannel.CREATOR.createFromParcel(parcel)
        } catch (_: Throwable) {
            null
        } finally {
            parcel.recycle()
        }
    }
}