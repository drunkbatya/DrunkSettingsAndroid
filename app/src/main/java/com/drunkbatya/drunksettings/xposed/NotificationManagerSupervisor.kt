package com.drunkbatya.drunksettings.xposed

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Process
import android.os.SystemClock
import android.os.VibrationEffect
import android.view.KeyEvent
import com.drunkbatya.drunksettings.ui.model.NotifDetectMode
import com.drunkbatya.drunksettings.xposed.hookers.AddServiceHooker
import com.drunkbatya.drunksettings.xposed.hookers.AreNotificationsEnabledHooker
import com.drunkbatya.drunksettings.xposed.hookers.BlinkHooker
import com.drunkbatya.drunksettings.xposed.hookers.CaptureDisplayHooker
import com.drunkbatya.drunksettings.xposed.hookers.HeadsetKeyHooker
import com.drunkbatya.drunksettings.xposed.hookers.NotificationChannelHooker
import com.drunkbatya.drunksettings.xposed.hookers.RegisterContentObserverHooker
import com.drunkbatya.drunksettings.xposed.hookers.ScreenCaptureRegisterHooker
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
    private val packageManagerRef = AtomicReference<PackageManager?>()
    @Volatile
    private var notificationHooksInstalled = false

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
        val logger = { message: String -> log(TAG + message) }
        installNotificationAttentionHooks(classLoader, logger)
        installHeadsetButtonHooks(classLoader, logger)
        installNotificationServiceWatcher(classLoader, logger)
        installScreenshotDetectionHooks(classLoader, logger)
        installMediaStoreObserverHooks(classLoader, logger)
        installSecureCaptureHooks(classLoader, logger)
    }

    private fun installNotificationAttentionHooks(classLoader: ClassLoader, logger: (String) -> Unit) {
        val className = "com.android.server.notification.NotificationAttentionHelper"
        val attentionClass = XposedHelpers.findClass(className, classLoader)
        if (attentionClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        //hookTargetMethod(attentionClass, "buzzBeepBlinkLocked", BuzzBeepBlinkHooker::class.java)
        XposedHelpers.hookTargetMethod(this, attentionClass, "playSound", SoundHooker::class.java, logger)
        XposedHelpers.hookTargetMethod(this, attentionClass, "playVibration", VibrationHooker::class.java, logger)
        XposedHelpers.hookTargetMethod(this, attentionClass, "canShowLightsLocked", BlinkHooker::class.java, logger)
        log(TAG + "hooks installed for ${attentionClass.name}")
    }

    private fun installHeadsetButtonHooks(classLoader: ClassLoader, logger: (String) -> Unit) {
        val className = "com.android.server.policy.PhoneWindowManager"
        val pwmClass = XposedHelpers.findClass(className, classLoader)
        if (pwmClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        XposedHelpers.hookTargetMethod(
            this, pwmClass, "interceptKeyBeforeQueueing", HeadsetKeyHooker::class.java, logger
        )
        log(TAG + "hooks installed for ${pwmClass.name}")
    }

    /**
     * The notification-status APIs (areNotificationsEnabled / getNotificationChannel) live on the
     * INotificationManager.Stub instance, not on the NotificationManagerService class itself, so the
     * concrete method names differ per ROM. We watch ServiceManager.addService("notification", ...)
     * and hook the real binder's class once it is published.
     */
    private fun installNotificationServiceWatcher(classLoader: ClassLoader, logger: (String) -> Unit) {
        val className = "android.os.ServiceManager"
        val smClass = XposedHelpers.findClass(className, classLoader)
        if (smClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        XposedHelpers.hookTargetMethod(this, smClass, "addService", AddServiceHooker::class.java, logger)
        log(TAG + "watching ServiceManager.addService for notification service")
    }

    private fun installNotificationHooksOnStub(stubClass: Class<*>, logger: (String) -> Unit) {
        XposedHelpers.hookTargetMethod(
            this, stubClass, "areNotificationsEnabled", AreNotificationsEnabledHooker::class.java, logger
        )
        XposedHelpers.hookTargetMethod(
            this, stubClass, "areNotificationsEnabledForPackage",
            AreNotificationsEnabledHooker::class.java, logger
        )
        XposedHelpers.hookTargetMethod(
            this, stubClass, "getNotificationChannel", NotificationChannelHooker::class.java, logger
        )
        XposedHelpers.hookTargetMethod(
            this, stubClass, "getNotificationChannelForPackage",
            NotificationChannelHooker::class.java, logger
        )
        log(TAG + "notification detection hooks installed on ${stubClass.name}")
    }

    private fun installMediaStoreObserverHooks(classLoader: ClassLoader, logger: (String) -> Unit) {
        val className = "com.android.server.content.ContentService"
        val contentServiceClass = XposedHelpers.findClass(className, classLoader)
        if (contentServiceClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        XposedHelpers.hookTargetMethod(
            this, contentServiceClass, "registerContentObserver",
            RegisterContentObserverHooker::class.java, logger
        )
        log(TAG + "hooks installed for ${contentServiceClass.name}")
    }

    /**
     * Screenshots of FLAG_SECURE / SurfaceView.setSecure content come out black because the screen
     * capture is requested with captureSecureLayers=false. SysUI delegates the capture to
     * WindowManagerService.captureDisplay in system_server, so we flip the CaptureArgs there.
     */
    private fun installSecureCaptureHooks(classLoader: ClassLoader, logger: (String) -> Unit) {
        val className = "com.android.server.wm.WindowManagerService"
        val wmsClass = XposedHelpers.findClass(className, classLoader)
        if (wmsClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        XposedHelpers.hookTargetMethod(
            this, wmsClass, "captureDisplay", CaptureDisplayHooker::class.java, logger
        )
        log(TAG + "hooks installed for ${wmsClass.name}")
    }

    private fun installScreenshotDetectionHooks(classLoader: ClassLoader, logger: (String) -> Unit) {
        val className = "com.android.server.wm.ActivityRecord"
        val activityRecordClass = XposedHelpers.findClass(className, classLoader)
        if (activityRecordClass == null) {
            log(TAG + "failed to found class: $className")
            return
        }
        // Block registration of the screen-capture observer and, defensively, its dispatch.
        XposedHelpers.hookTargetMethod(
            this, activityRecordClass, "registerCaptureObserver",
            ScreenCaptureRegisterHooker::class.java, logger
        )
        XposedHelpers.hookTargetMethod(
            this, activityRecordClass, "reportScreenCaptured",
            ScreenCaptureRegisterHooker::class.java, logger
        )
        log(TAG + "hooks installed for ${activityRecordClass.name}")
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
        if (XposedHelpers.isMusicPlaying(systemContextRef.get())
                && modulePreferences.shouldPreventFadeOutSound()
            ) {
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
        if (!modulePreferences.shouldMuteBlinkWithSound()) {
            return
        }
        val record = XposedHelpers.findNotificationRecord(callback.args) ?: return
        val packageName = XposedHelpers.extractPackageName(record) ?: return
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting blink for $packageName, reason: rateLimit")
            callback.returnAndSkip(false)
            return
        }
        if (XposedHelpers.isMusicPlaying(systemContextRef.get())
            && modulePreferences.shouldPreventFadeOutSound())
        {
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

    internal fun onHeadsetKeyBefore(callback: XposedInterface.BeforeHookCallback) {
        if (!modulePreferences.shouldSwallowHeadsetButton()) {
            return
        }
        val event = XposedHelpers.findKeyEvent(callback.args) ?: return
        val keyCode = event.keyCode
        if (keyCode !in HEADSET_KEYCODES) {
            return
        }
        logVerbose(
            TAG + "headset key seen: code=$keyCode device=${event.deviceId} " +
                "source=${event.source} action=${event.action}"
        )
        // Wired headset hardware buttons arrive from a real input device (deviceId >= 0);
        // Bluetooth/UI media events are injected with a virtual device id (-1).
        if (event.deviceId < 0) {
            return
        }
        log(TAG + "swallowing wired headset key: code=$keyCode device=${event.deviceId}")
        callback.returnAndSkip(0)
    }

    /**
     * Only spoof the value handed back to an app querying over Binder. Internal callers inside
     * system_server (notification delivery) run with our own pid, so leaving those untouched keeps
     * real delivery behaviour intact — we only hide the state from the asking app.
     */
    private fun isExternalCaller(): Boolean {
        return Binder.getCallingPid() != Process.myPid()
    }

    internal fun onAreNotificationsEnabledBefore(callback: XposedInterface.BeforeHookCallback) {
        if (!isExternalCaller()) {
            return
        }
        val packageName = XposedHelpers.firstStringArg(callback.args) ?: return
        when (modulePreferences.resolveNotifDetectMode(packageName)) {
            NotifDetectMode.FAKE_ON -> {
                log(TAG + "notif detect: faking ENABLED for $packageName")
                callback.returnAndSkip(true)
            }
            NotifDetectMode.FAKE_OFF -> {
                log(TAG + "notif detect: faking DISABLED for $packageName")
                callback.returnAndSkip(false)
            }
            NotifDetectMode.DIRECT -> {}
        }
    }

    internal fun onNotificationChannelAfter(callback: XposedInterface.AfterHookCallback) {
        if (!isExternalCaller()) {
            return
        }
        val packageName = XposedHelpers.firstStringArg(callback.args) ?: return
        val mode = modulePreferences.resolveNotifDetectMode(packageName)
        if (mode == NotifDetectMode.DIRECT) {
            return
        }
        val channel = callback.result as? NotificationChannel ?: return
        val importance = channel.importance
        val targetImportance = when (mode) {
            NotifDetectMode.FAKE_ON ->
                if (importance == NotificationManager.IMPORTANCE_NONE) {
                    NotificationManager.IMPORTANCE_DEFAULT
                } else {
                    return
                }
            NotifDetectMode.FAKE_OFF ->
                if (importance != NotificationManager.IMPORTANCE_NONE) {
                    NotificationManager.IMPORTANCE_NONE
                } else {
                    return
                }
            NotifDetectMode.DIRECT -> return
        }
        val copy = XposedHelpers.cloneNotificationChannel(channel) ?: return
        copy.importance = targetImportance
        callback.setResult(copy)
        log(TAG + "notif detect: channel importance $importance -> $targetImportance for $packageName")
    }

    internal fun onScreenCaptureRegisterBefore(callback: XposedInterface.BeforeHookCallback) {
        val packageName = XposedHelpers.extractActivityPackageName(callback.thisObject) ?: return
        logVerbose(TAG + "screen capture hook for $packageName via ${callback.member}")
        if (modulePreferences.shouldBlockScreenshotDetection(packageName)) {
            log(TAG + "blocking screenshot detection for $packageName")
            callback.returnAndSkip(null)
        }
    }

    internal fun onAddServiceBefore(callback: XposedInterface.BeforeHookCallback) {
        if (notificationHooksInstalled) {
            return
        }
        val name = callback.args.firstOrNull { it is String } as? String ?: return
        if (name != "notification") {
            return
        }
        val binder = callback.args.getOrNull(1) ?: return
        notificationHooksInstalled = true
        log(TAG + "notification service published, hooking ${binder.javaClass.name}")
        installNotificationHooksOnStub(binder.javaClass) { message -> log(TAG + message) }
    }

    /**
     * Legacy screenshot detection (Telegram et al.) registers a ContentObserver on
     * MediaStore.Images and reacts to new files named "screenshot". All ContentObserver
     * registrations funnel through ContentService in system_server, so we drop the registration of
     * MediaStore-image observers for targeted apps — their observer then never fires.
     */
    internal fun onRegisterContentObserverBefore(callback: XposedInterface.BeforeHookCallback) {
        val uri = callback.args.firstOrNull { it is Uri } as? Uri ?: return
        if (!isMediaStoreImagesUri(uri)) {
            return
        }
        val uid = Binder.getCallingUid()
        val packageManager = packageManagerFrom(callback.thisObject) ?: return
        val packages = packageManager.getPackagesForUid(uid) ?: return
        val blocked = packages.firstOrNull { modulePreferences.shouldBlockScreenshotDetection(it) }
        logVerbose(
            TAG + "registerContentObserver media uri=$uri uid=$uid " +
                "pkgs=${packages.joinToString()} blocked=$blocked"
        )
        if (blocked != null) {
            log(TAG + "blocking MediaStore screenshot observer for $blocked (uid=$uid)")
            callback.returnAndSkip(null)
        }
    }

    internal fun onCaptureDisplayBefore(callback: XposedInterface.BeforeHookCallback) {
        if (!modulePreferences.shouldCaptureSecureLayers()) {
            return
        }
        // WindowManagerService.captureDisplay(int displayId, CaptureArgs captureArgs, listener)
        val captureArgs = callback.args.firstOrNull {
            it != null && it.javaClass.name.endsWith("CaptureArgs")
        }
        if (captureArgs == null) {
            logVerbose(TAG + "captureDisplay with null CaptureArgs, cannot force secure layers")
            return
        }
        val applied = XposedHelpers.setBooleanField(captureArgs, "mCaptureSecureLayers", true)
        logVerbose(TAG + "captureDisplay: forcing captureSecureLayers=true (applied=$applied)")
    }

    private fun isMediaStoreImagesUri(uri: Uri): Boolean {
        val authority = uri.authority ?: return false
        // authority is "media" or, for cross-user, "<userId>@media"
        if (authority != "media" && !authority.endsWith("@media")) {
            return false
        }
        val path = uri.path ?: return false
        return path.contains("images") || path.contains("image")
    }

    private fun packageManagerFrom(instance: Any?): PackageManager? {
        packageManagerRef.get()?.let { return it }
        val context = XposedHelpers.readContextField(instance) ?: systemContextRef.get() ?: return null
        val pm = context.packageManager ?: return null
        packageManagerRef.compareAndSet(null, pm)
        return pm
    }

    fun logVerbose(message: String) {
        if (modulePreferences.isVerboseLogging()) {
            log(message)
        }
    }
    companion object {
        private const val TAG = "DrunkSettings: "
        private val HEADSET_KEYCODES = setOf(
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND,
        )
    }

}
