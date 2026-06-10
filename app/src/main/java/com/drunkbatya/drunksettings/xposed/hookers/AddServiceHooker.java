package com.drunkbatya.drunksettings.xposed.hookers;

import com.drunkbatya.drunksettings.xposed.ModuleBridge;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@XposedHooker
public final class AddServiceHooker implements XposedInterface.Hooker {
    @BeforeInvocation
    public static void before(XposedInterface.BeforeHookCallback callback) {
        ModuleBridge.onAddServiceBefore(callback);
    }
}
