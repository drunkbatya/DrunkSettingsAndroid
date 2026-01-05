package com.drunkbatya.drunksettings.xposed

import io.github.libxposed.api.XposedInterface

object ModuleBridge {
    @JvmField
    var moduleInstance: NotificationManagerSupervisor? = null

    @JvmStatic
    fun onSoundVibrationBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onSoundBefore(callback)
    }

    @JvmStatic
    fun onBlinkBefore(callback: XposedInterface.BeforeHookCallback) {
        moduleInstance?.onSoundBefore(callback)
    }
}
