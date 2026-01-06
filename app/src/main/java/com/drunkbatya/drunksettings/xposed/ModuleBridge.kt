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
}
