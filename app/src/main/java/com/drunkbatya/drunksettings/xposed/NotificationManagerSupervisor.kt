package com.drunkbatya.drunksettings.xposed

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.SystemClock
import android.os.VibrationEffect
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap

class NotificationManagerSupervisor(
    base: XposedInterface,
    param: XposedModuleInterface.ModuleLoadedParam,
) : XposedModule(base, param) {
    private val lastSoundByPackage = ConcurrentHashMap<String, Long>()
    private val systemContextRef = AtomicReference<Context?>()

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
        hookTargetMethod(attentionClass, "playSound", SoundHooker::class.java)
        hookTargetMethod(attentionClass, "playVibration", VibrationHooker::class.java)
        hookTargetMethod(attentionClass, "canShowLightsLocked", BlinkHooker::class.java)
        log(TAG + "hooks installed for ${attentionClass.name}")
    }

    private fun hookTargetMethod(
        targetClass: Class<*>,
        targetMethod: String,
        hooker: Class<out XposedInterface.Hooker>
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
                hook(method, hooker)
                hooked += 1
            }
            if (hooked == 0) {
                log(TAG + "method not found: $targetMethod in ${targetClass.name}")
            } else {
                log(TAG + "successfully hooked $hooked method(s): $targetMethod in ${targetClass.name}")
            }
        } catch (_: SecurityException) {
            log(TAG + "security exception while hooking $targetMethod in ${targetClass.name}")
        } catch (e: Throwable) {
            log(TAG + "unexpected error while hooking $targetMethod in ${targetClass.name}: ${e.message}")
        }
    }

    private fun isMusicPlaying(): Boolean {
        val context = systemContextRef.get() ?: return false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        return audioManager.isMusicActive
    }

    private fun resolveLimitSeconds(packageName: String): Int {
        val generalValue = prefs.getInt(
            "general_min_notification_sound_timeout",
            0
        )
        val appKey = "min_notification_sound_timeout_$packageName"
        val appValue = prefs.getInt(appKey, generalValue)
        //log(TAG + "prefs for $packageName appKey=$appKey appValue=$appValue generalValue=$generalValue")
        return appValue
    }

    private fun shouldPreventFadeOutSound(): Boolean {
        return prefs.getBoolean("general_do_not_fade_out_music", false)
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

    private fun findClass(
        @Suppress("SameParameterValue") name: String,
        classLoader: ClassLoader
    ): Class<*>? {
        return runCatching { Class.forName(name, false, classLoader) }.getOrNull()
    }

    internal fun onSoundBefore(callback: XposedInterface.BeforeHookCallback) {
        updateSystemContext(callback.thisObject)
        val method = callback.member as? Method ?: return
        if (method.returnType != Boolean::class.javaPrimitiveType &&
            method.returnType != java.lang.Boolean::class.java
        ) {
            log(TAG + "unknown method: $method")
            return
        }
        val record = findNotificationRecord(callback.args) ?: return
        val packageName = extractPackageName(record) ?: return
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting sound for $packageName, reason: rateLimit")
            callback.returnAndSkip(false)
            return
        }
        if (isMusicPlaying() && shouldPreventFadeOutSound()) {
            log(TAG + "replacing sound for $packageName to vibration, reason: musicPlaying")
            val helper = callback.thisObject ?: return
            val vibration = record.javaClass.getMethod("getVibration").invoke(record) as? VibrationEffect
            if (vibration == null) {
                val method = helper.javaClass.declaredMethods.firstOrNull {
                    it.name == "playVibration" && it.parameterTypes.size == 3
                }?: return
                callback.returnAndSkip(false)
                method.invoke(helper, record, VibrationEffect.createOneShot(500, 255), false)
            } else {
                callback.returnAndSkip(false)
            }
            return
        }
        lastSoundByPackage[packageName] = SystemClock.elapsedRealtime()
    }

    internal fun onVibrationBefore(callback: XposedInterface.BeforeHookCallback) {
        updateSystemContext(callback.thisObject)
        val method = callback.member as? Method ?: return
        if (method.returnType != Boolean::class.javaPrimitiveType &&
            method.returnType != java.lang.Boolean::class.java
        ) {
            log(TAG + "unknown method: $method")
            return
        }
        val record = findNotificationRecord(callback.args) ?: return
        val packageName = extractPackageName(record) ?: return
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting vibration for $packageName, reason: rateLimit")
            callback.returnAndSkip(false)
            return
        }
        lastSoundByPackage[packageName] = SystemClock.elapsedRealtime()
    }

    internal fun onBlinkBefore(callback: XposedInterface.BeforeHookCallback) {
        updateSystemContext(callback.thisObject)
        val method = callback.member as? Method ?: return
        if (method.returnType != Boolean::class.javaPrimitiveType &&
            method.returnType != java.lang.Boolean::class.java
        ) {
            log(TAG + "unknown method: $method")
            return
        }
        val record = findNotificationRecord(callback.args) ?: return
        val packageName = extractPackageName(record) ?: return
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting blink for $packageName, reason: rateLimit")
            callback.returnAndSkip(false)
            return
        }
        if (isMusicPlaying() && shouldPreventFadeOutSound()) {
            log(TAG + "muting blink for $packageName, reason: musicPlaying")
            callback.returnAndSkip(false)
            return
        }
        lastSoundByPackage[packageName] = SystemClock.elapsedRealtime()
    }

    private fun updateSystemContext(helper: Any?) {
        if (helper == null || systemContextRef.get() != null) {
            return
        }
        val context = runCatching {
            helper.javaClass.getDeclaredField("mContext").apply {
                isAccessible = true
            }.get(helper) as? Context
        }.getOrNull()
        if (context != null) {
            systemContextRef.compareAndSet(null, context)
        }
    }
    companion object {
        private const val TAG = "DrunkSettings: "
    }
}

