package com.drunkbatya.drunksettings.xposed.hookers;

import com.drunkbatya.drunksettings.xposed.ModuleBridge;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@XposedHooker
public final class NotificationChannelHooker implements XposedInterface.Hooker {
    @AfterInvocation
    public static void after(XposedInterface.AfterHookCallback callback) {
        ModuleBridge.onNotificationChannelAfter(callback);
    }
}
