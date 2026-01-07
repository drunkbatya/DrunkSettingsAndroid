package com.drunkbatya.drunksettings.xposed.hookers;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.XposedHooker;
import io.github.libxposed.api.annotations.AfterInvocation;

@XposedHooker
public final class BuzzBeepBlinkHooker implements XposedInterface.Hooker {
    @AfterInvocation
    public static void after(XposedInterface.AfterHookCallback callback) {
        //ModuleBridge.onBuzzBeepAfter(callback);
    }
}
