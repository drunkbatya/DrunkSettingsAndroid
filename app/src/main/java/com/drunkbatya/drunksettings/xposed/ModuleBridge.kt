package com.drunkbatya.drunksettings.xposed

import io.github.libxposed.api.XposedInterface

object ModuleBridge {
    @JvmField
    var moduleInstance: NotificationManagerSupervisor? = null

    @JvmStatic
    fun onSoundBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onSoundBefore(callback)
    }

    @JvmStatic
    fun onVibrationBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onVibrationBefore(callback)
    }

    @JvmStatic
    fun onBlinkBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onBlinkBefore(callback)
    }

    @JvmStatic
    fun onHeadsetKeyBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onHeadsetKeyBefore(callback)
    }

    @JvmStatic
    fun onAreNotificationsEnabledBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onAreNotificationsEnabledBefore(callback)
    }

    @JvmStatic
    fun onNotificationChannelAfter(callback: XposedInterface.AfterHookCallback) {
        moduleInstance?.onNotificationChannelAfter(callback)
    }

    @JvmStatic
    fun onScreenCaptureRegisterBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onScreenCaptureRegisterBefore(callback)
    }

    @JvmStatic
    fun onAddServiceBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onAddServiceBefore(callback)
    }

    @JvmStatic
    fun onRegisterContentObserverBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onRegisterContentObserverBefore(callback)
    }

    @JvmStatic
    fun onCaptureDisplayBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onCaptureDisplayBefore(callback)
    }
}
