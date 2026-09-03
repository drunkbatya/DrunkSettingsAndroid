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
import android.util.Log
import android.view.KeyEvent
import com.drunkbatya.drunksettings.data.SettingsKeys
import com.drunkbatya.drunksettings.ui.model.NotifDetectMode
import com.drunkbatya.drunksettings.xposed.helpers.XposedHelpers
import com.drunkbatya.drunksettings.xposed.preferences.ModulePreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap

class NotificationManagerSupervisor : XposedModule() {
    private val lastSoundByPackage = ConcurrentHashMap<String, Long>()
    private val systemContextRef = AtomicReference<Context?>()
    private val packageManagerRef = AtomicReference<PackageManager?>()
    @Volatile
    private var notificationHooksInstalled = false

    private lateinit var prefs: SharedPreferences
    private val modulePreferences = ModulePreferences(
        log = { log(it) },
        logVerbose = { logVerbose(it) },
    )
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        if (shared != null) {
            modulePreferences.onPreferenceChanged(shared, key)
        }
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        log(TAG + "onModuleLoaded: ${param.processName}")
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(TAG + "onSystemServerStarting")
        prefs = getRemotePreferences(SettingsKeys.PREFS_NAME)
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
        XposedHelpers.hookTargetMethod(this, attentionClass, "playSound", { onSound(it) }, logger)
        XposedHelpers.hookTargetMethod(this, attentionClass, "playVibration", { onVibration(it) }, logger)
        XposedHelpers.hookTargetMethod(this, attentionClass, "canShowLightsLocked", { onBlink(it) }, logger)
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
            this, pwmClass, "interceptKeyBeforeQueueing", { onHeadsetKey(it) }, logger
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
        XposedHelpers.hookTargetMethod(this, smClass, "addService", { onAddService(it) }, logger)
        log(TAG + "watching ServiceManager.addService for notification service")
    }

    private fun installNotificationHooksOnStub(stubClass: Class<*>, logger: (String) -> Unit) {
        val notifEnabled = XposedInterface.Hooker { onAreNotificationsEnabled(it) }
        val channel = XposedInterface.Hooker { onNotificationChannel(it) }
        XposedHelpers.hookTargetMethod(this, stubClass, "areNotificationsEnabled", notifEnabled, logger)
        XposedHelpers.hookTargetMethod(
            this, stubClass, "areNotificationsEnabledForPackage", notifEnabled, logger
        )
        XposedHelpers.hookTargetMethod(this, stubClass, "getNotificationChannel", channel, logger)
        XposedHelpers.hookTargetMethod(this, stubClass, "getNotificationChannelForPackage", channel, logger)
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
            this, contentServiceClass, "registerContentObserver", { onRegisterContentObserver(it) }, logger
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
        XposedHelpers.hookTargetMethod(this, wmsClass, "captureDisplay", { onCaptureDisplay(it) }, logger)
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
        val hooker = XposedInterface.Hooker { onScreenCaptureRegister(it) }
        XposedHelpers.hookTargetMethod(this, activityRecordClass, "registerCaptureObserver", hooker, logger)
        XposedHelpers.hookTargetMethod(this, activityRecordClass, "reportScreenCaptured", hooker, logger)
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

    private fun returnsBoolean(chain: Chain): Boolean {
        val method = chain.executable as? Method ?: return true
        return method.returnType == Boolean::class.javaPrimitiveType ||
            method.returnType == java.lang.Boolean::class.java
    }

    private fun onSound(chain: Chain): Any? {
        updateSystemContext(chain.thisObject)
        if (!returnsBoolean(chain)) {
            log(TAG + "unknown method: ${chain.executable}")
            return chain.proceed()
        }
        val record = XposedHelpers.findNotificationRecord(chain.args) ?: return chain.proceed()
        val packageName = XposedHelpers.extractPackageName(record) ?: return chain.proceed()
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting sound for $packageName, reason: rateLimit")
            return false
        }
        if (XposedHelpers.isMusicPlaying(systemContextRef.get()) &&
            modulePreferences.shouldPreventFadeOutSound()
        ) {
            log(TAG + "replacing sound for $packageName to vibration, reason: musicPlaying")
            val helper = chain.thisObject ?: return chain.proceed()
            val vibration = record.javaClass.getMethod("getVibration").invoke(record) as? VibrationEffect
            if (vibration == null) {
                val playVibration = helper.javaClass.declaredMethods.firstOrNull {
                    it.name == "playVibration" && it.parameterTypes.size == 3
                } ?: return chain.proceed()
                playVibration.invoke(helper, record, VibrationEffect.createOneShot(500, 255), false)
            }
            return false
        }
        lastSoundByPackage[packageName] = SystemClock.elapsedRealtime()
        return chain.proceed()
    }

    private fun onVibration(chain: Chain): Any? {
        updateSystemContext(chain.thisObject)
        if (!returnsBoolean(chain)) {
            log(TAG + "unknown method: ${chain.executable}")
            return chain.proceed()
        }
        val record = XposedHelpers.findNotificationRecord(chain.args) ?: return chain.proceed()
        val packageName = XposedHelpers.extractPackageName(record) ?: return chain.proceed()
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting vibration for $packageName, reason: rateLimit")
            return false
        }
        lastSoundByPackage[packageName] = SystemClock.elapsedRealtime()
        return chain.proceed()
    }

    private fun onBlink(chain: Chain): Any? {
        updateSystemContext(chain.thisObject)
        if (!returnsBoolean(chain)) {
            log(TAG + "unknown method: ${chain.executable}")
            return chain.proceed()
        }
        if (!modulePreferences.shouldMuteBlinkWithSound()) {
            return chain.proceed()
        }
        val record = XposedHelpers.findNotificationRecord(chain.args) ?: return chain.proceed()
        val packageName = XposedHelpers.extractPackageName(record) ?: return chain.proceed()
        if (shouldMuteNow(packageName)) {
            log(TAG + "muting blink for $packageName, reason: rateLimit")
            return false
        }
        if (XposedHelpers.isMusicPlaying(systemContextRef.get()) &&
            modulePreferences.shouldPreventFadeOutSound()
        ) {
            log(TAG + "muting blink for $packageName, reason: musicPlaying")
            return false
        }
        lastSoundByPackage[packageName] = SystemClock.elapsedRealtime()
        return chain.proceed()
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

    private fun onHeadsetKey(chain: Chain): Any? {
        if (!modulePreferences.shouldSwallowHeadsetButton()) {
            return chain.proceed()
        }
        val event = XposedHelpers.findKeyEvent(chain.args) ?: return chain.proceed()
        val keyCode = event.keyCode
        if (keyCode !in HEADSET_KEYCODES) {
            return chain.proceed()
        }
        logVerbose(
            TAG + "headset key seen: code=$keyCode device=${event.deviceId} " +
                "source=${event.source} action=${event.action}"
        )
        // Wired headset hardware buttons arrive from a real input device (deviceId >= 0);
        // Bluetooth/UI media events are injected with a virtual device id (-1).
        if (event.deviceId < 0) {
            return chain.proceed()
        }
        log(TAG + "swallowing wired headset key: code=$keyCode device=${event.deviceId}")
        return 0
    }

    /**
     * Only spoof the value handed back to an app querying over Binder. Internal callers inside
     * system_server (notification delivery) run with our own pid, so leaving those untouched keeps
     * real delivery behaviour intact — we only hide the state from the asking app.
     */
    private fun isExternalCaller(): Boolean {
        return Binder.getCallingPid() != Process.myPid()
    }

    private fun onAreNotificationsEnabled(chain: Chain): Any? {
        if (!isExternalCaller()) {
            return chain.proceed()
        }
        val packageName = XposedHelpers.firstStringArg(chain.args) ?: return chain.proceed()
        return when (modulePreferences.resolveNotifDetectMode(packageName)) {
            NotifDetectMode.FAKE_ON -> {
                log(TAG + "notif detect: faking ENABLED for $packageName")
                true
            }
            NotifDetectMode.FAKE_OFF -> {
                log(TAG + "notif detect: faking DISABLED for $packageName")
                false
            }
            NotifDetectMode.DIRECT -> chain.proceed()
        }
    }

    private fun onNotificationChannel(chain: Chain): Any? {
        val result = chain.proceed()
        if (!isExternalCaller()) {
            return result
        }
        val packageName = XposedHelpers.firstStringArg(chain.args) ?: return result
        val mode = modulePreferences.resolveNotifDetectMode(packageName)
        if (mode == NotifDetectMode.DIRECT) {
            return result
        }
        val channel = result as? NotificationChannel ?: return result
        val importance = channel.importance
        val targetImportance = when (mode) {
            NotifDetectMode.FAKE_ON ->
                if (importance == NotificationManager.IMPORTANCE_NONE) {
                    NotificationManager.IMPORTANCE_DEFAULT
                } else {
                    return result
                }
            NotifDetectMode.FAKE_OFF ->
                if (importance != NotificationManager.IMPORTANCE_NONE) {
                    NotificationManager.IMPORTANCE_NONE
                } else {
                    return result
                }
            NotifDetectMode.DIRECT -> return result
        }
        val copy = XposedHelpers.cloneNotificationChannel(channel) ?: return result
        copy.importance = targetImportance
        log(TAG + "notif detect: channel importance $importance -> $targetImportance for $packageName")
        return copy
    }

    private fun onScreenCaptureRegister(chain: Chain): Any? {
        val packageName = XposedHelpers.extractActivityPackageName(chain.thisObject)
            ?: return chain.proceed()
        logVerbose(TAG + "screen capture hook for $packageName via ${chain.executable}")
        if (modulePreferences.shouldBlockScreenshotDetection(packageName)) {
            log(TAG + "blocking screenshot detection for $packageName")
            return null
        }
        return chain.proceed()
    }

    private fun onAddService(chain: Chain): Any? {
        if (notificationHooksInstalled) {
            return chain.proceed()
        }
        val name = chain.args.firstOrNull { it is String } as? String
        if (name != "notification") {
            return chain.proceed()
        }
        val binder = chain.args.getOrNull(1) ?: return chain.proceed()
        notificationHooksInstalled = true
        log(TAG + "notification service published, hooking ${binder.javaClass.name}")
        installNotificationHooksOnStub(binder.javaClass) { message -> log(TAG + message) }
        return chain.proceed()
    }

    /**
     * Legacy screenshot detection (Telegram et al.) registers a ContentObserver on
     * MediaStore.Images and reacts to new files named "screenshot". All ContentObserver
     * registrations funnel through ContentService in system_server, so we drop the registration of
     * MediaStore-image observers for targeted apps — their observer then never fires.
     */
    private fun onRegisterContentObserver(chain: Chain): Any? {
        val uri = chain.args.firstOrNull { it is Uri } as? Uri ?: return chain.proceed()
        if (!isMediaStoreImagesUri(uri)) {
            return chain.proceed()
        }
        val uid = Binder.getCallingUid()
        val packageManager = packageManagerFrom(chain.thisObject) ?: return chain.proceed()
        val packages = packageManager.getPackagesForUid(uid) ?: return chain.proceed()
        val blocked = packages.firstOrNull { modulePreferences.shouldBlockScreenshotDetection(it) }
        logVerbose(
            TAG + "registerContentObserver media uri=$uri uid=$uid " +
                "pkgs=${packages.joinToString()} blocked=$blocked"
        )
        if (blocked != null) {
            log(TAG + "blocking MediaStore screenshot observer for $blocked (uid=$uid)")
            return null
        }
        return chain.proceed()
    }

    private fun onCaptureDisplay(chain: Chain): Any? {
        if (!modulePreferences.shouldCaptureSecureLayers()) {
            return chain.proceed()
        }
        // WindowManagerService.captureDisplay(int displayId, CaptureArgs captureArgs, listener)
        val captureArgs = chain.args.firstOrNull {
            it != null && it.javaClass.name.endsWith("CaptureArgs")
        }
        if (captureArgs == null) {
            logVerbose(TAG + "captureDisplay with null CaptureArgs, cannot force secure layers")
            return chain.proceed()
        }
        val applied = XposedHelpers.setBooleanField(captureArgs, "mCaptureSecureLayers", true)
        logVerbose(TAG + "captureDisplay: forcing captureSecureLayers=true (applied=$applied)")
        return chain.proceed()
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

    fun log(message: String) {
        log(Log.INFO, TAG_LOGCAT, message)
    }

    fun logVerbose(message: String) {
        if (modulePreferences.isVerboseLogging()) {
            log(message)
        }
    }

    companion object {
        private const val TAG_LOGCAT = "DrunkSettings"
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
