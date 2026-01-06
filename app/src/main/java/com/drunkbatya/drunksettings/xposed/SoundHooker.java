package com.drunkbatya.drunksettings.xposed;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@XposedHooker
public final class SoundHooker implements XposedInterface.Hooker {
    @BeforeInvocation
    public static void before(XposedInterface.BeforeHookCallback callback) {
        ModuleBridge.onSoundBefore(callback);
    }

    //@AfterInvocation
    //public static void after(XposedInterface.AfterHookCallback callback) {
    //    ModuleBridge.onSoundVibrationAfter(callback);
    //}
}
