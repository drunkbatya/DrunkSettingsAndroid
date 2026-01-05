package com.drunkbatya.drunksettings.xposed

import android.content.SharedPreferences
import android.os.SystemClock
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class NotificationManagerSupervisor(
    base: XposedInterface,
    param: XposedModuleInterface.ModuleLoadedParam,
) : XposedModule(base, param) {
    private val lastSoundByPackage = ConcurrentHashMap<String, Long>()

    private lateinit var prefs: SharedPreferences

    init {
        ModuleBridge.moduleInstance = this
        log(TAG + "init done")
    }

    override fun onSystemServerLoaded(param: XposedModuleInterface.SystemServerLoadedParam) {
        log(TAG + "onSystemServerLoaded")
        prefs = getRemotePreferences("NotificationManagerSupervisor")
        installHooks(param.classLoader)
    }

    private fun installHooks(classLoader: ClassLoader) {
        val className = "com.android.server.notification.NotificationAttentionHelper"
        val attentionClass = findClass(className, classLoader)
        if (attentionClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        //hookTargetMethod(attentionClass, "buzzBeepBlinkLocked", BuzzBeepBlinkHooker::class.java)
        hookTargetMethod(attentionClass, "playSound", SoundVibrationHooker::class.java)
        hookTargetMethod(attentionClass, "playVibration", SoundVibrationHooker::class.java)
        hookTargetMethod(attentionClass, "canShowLightsLocked", BlinkHooker::class.java)
        log(TAG + "hooks installed for ${attentionClass.name}")
        return
    }

    private fun hookTargetMethod(targetClass: Class<*>, targetMethod: String, hooker: Class<out XposedInterface.Hooker>) {
        try {
            val method = targetClass.getDeclaredMethod(targetMethod)
            hook(method, hooker)
            log(TAG + "successfully hooked method: $targetMethod in ${targetClass.name}")

        } catch (e: NoSuchMethodException) {
            log(TAG + "method not found: $targetMethod in ${targetClass.name}: $e")

        } catch (e: SecurityException) {
            log(TAG + "security exception while hooking $targetMethod in ${targetClass.name}: $e")

        } catch (e: Throwable) {
            log(TAG + "unexpected error while hooking $targetMethod in ${targetClass.name}: ${e.message}")
        }
    }

    private fun resolveLimitSeconds(packageName: String): Int {
        val generalValue = prefs.getInt(
            "general_min_notification_sound_timeout",
            0
        )
        val appKey = "min_notification_sound_timeout_$packageName"
        val appValue = prefs.getInt(appKey, generalValue)
        log("DrunkSettings: prefs for $packageName appKey=$appKey appValue=$appValue generalValue=$generalValue")
        return appValue
    }

    private fun shouldMuteNow(packageName: String): Boolean {
        val limitSeconds = resolveLimitSeconds(packageName)
        if (limitSeconds <= 0) {
            return false
        }

        val now = SystemClock.elapsedRealtime()
        val last = lastSoundByPackage[packageName] ?: return false
        return now - last < limitSeconds * 1000L
    }

    private fun extractPackageName(record: Any): String? {
        val sbn = runCatching { record.javaClass.getMethod("getSbn").invoke(record) }.getOrNull()
            ?: runCatching {
                record.javaClass.getDeclaredField("sbn").apply { isAccessible = true }.get(record)
            }.getOrNull()
        return runCatching { sbn?.javaClass?.getMethod("getPackageName")?.invoke(sbn) as? String }
            .getOrNull()
    }

    private fun findNotificationRecord(args: Array<Any?>): Any? {
        return args.firstOrNull { arg ->
            arg != null && arg.javaClass.name.endsWith("NotificationRecord")
        }
    }

    private fun findClass(name: String, classLoader: ClassLoader): Class<*>? {
        return runCatching { Class.forName(name, false, classLoader) }.getOrNull()
    }

    internal fun onSoundBefore(callback: XposedInterface.BeforeHookCallback) {
        val method = callback.member as? Method ?: return
        if (method.returnType != Boolean::class.javaPrimitiveType &&
            method.returnType != java.lang.Boolean::class.java
        ) {
            return
        }
        val record = findNotificationRecord(callback.args) ?: return
        val packageName = extractPackageName(record) ?: return
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting notification for $packageName")
            callback.returnAndSkip(false)
            return
        }
        lastSoundByPackage[packageName] = SystemClock.elapsedRealtime()
    }
    companion object {
        private const val TAG = "DrunkSettings: "
    }
}




