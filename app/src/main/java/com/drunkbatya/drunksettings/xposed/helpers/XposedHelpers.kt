package com.drunkbatya.drunksettings.xposed.helpers

import android.content.Context
import android.media.AudioManager
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
        hooker: Class<out XposedInterface.Hooker>,
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
                module.hook(method, hooker)
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

    fun findNotificationRecord(args: Array<Any?>): Any? {
        return args.firstOrNull { arg ->
            arg != null && arg.javaClass.name.endsWith("NotificationRecord")
        }
    }

    fun findClass(
        @Suppress("SameParameterValue") name: String,
        classLoader: ClassLoader
    ): Class<*>? {
        return runCatching { Class.forName(name, false, classLoader) }.getOrNull()
    }
}