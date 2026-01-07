package com.drunkbatya.drunksettings.xposed

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.os.VibrationEffect
import com.drunkbatya.drunksettings.xposed.hookers.BlinkHooker
import com.drunkbatya.drunksettings.xposed.hookers.SoundHooker
import com.drunkbatya.drunksettings.xposed.hookers.VibrationHooker
import com.drunkbatya.drunksettings.xposed.helpers.XposedHelpers
import com.drunkbatya.drunksettings.xposed.preferences.ModulePreferences
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
    private val modulePreferences = ModulePreferences()
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        if (shared != null) {
            modulePreferences.onPreferenceChanged(shared, key)
        }
    }

    init {
        ModuleBridge.moduleInstance = this
        log(TAG + "init done")
    }

    override fun onSystemServerLoaded(param: XposedModuleInterface.SystemServerLoadedParam) {
        log(TAG + "onSystemServerLoaded")
        prefs = getRemotePreferences(ModulePreferences.PREFS_NAME)
        modulePreferences.syncAll(prefs)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        installHooks(param.classLoader)
    }

    private fun installHooks(classLoader: ClassLoader) {
        val className = "com.android.server.notification.NotificationAttentionHelper"
        val attentionClass = XposedHelpers.findClass(className, classLoader)
        if (attentionClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        val logger = { message: String -> log(TAG + message) }
        //hookTargetMethod(attentionClass, "buzzBeepBlinkLocked", BuzzBeepBlinkHooker::class.java)
        XposedHelpers.hookTargetMethod(this, attentionClass, "playSound", SoundHooker::class.java, logger)
        XposedHelpers.hookTargetMethod(this, attentionClass, "playVibration", VibrationHooker::class.java, logger)
        XposedHelpers.hookTargetMethod(this, attentionClass, "canShowLightsLocked", BlinkHooker::class.java, logger)
        log(TAG + "hooks installed for ${attentionClass.name}")
    }

    private fun shouldPreventFadeOutSound(): Boolean {
        return modulePreferences.shouldPreventFadeOutSound()
    }

    private fun shouldMuteNow(packageName: String): Boolean {
        val limitSeconds = modulePreferences.resolveLimitSeconds(packageName)
        if (limitSeconds <= 0) {
            return false
        }

        val now = SystemClock.elapsedRealtime()
        val last = lastSoundByPackage[packageName] ?: return false
        return now - last < limitSeconds * 1000L
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
        val record = XposedHelpers.findNotificationRecord(callback.args) ?: return
        val packageName = XposedHelpers.extractPackageName(record) ?: return
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting sound for $packageName, reason: rateLimit")
            callback.returnAndSkip(false)
            return
        }
        if (XposedHelpers.isMusicPlaying(systemContextRef.get()) && shouldPreventFadeOutSound()) {
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
        val record = XposedHelpers.findNotificationRecord(callback.args) ?: return
        val packageName = XposedHelpers.extractPackageName(record) ?: return
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
        val record = XposedHelpers.findNotificationRecord(callback.args) ?: return
        val packageName = XposedHelpers.extractPackageName(record) ?: return
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting blink for $packageName, reason: rateLimit")
            callback.returnAndSkip(false)
            return
        }
        if (XposedHelpers.isMusicPlaying(systemContextRef.get()) && shouldPreventFadeOutSound()) {
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
