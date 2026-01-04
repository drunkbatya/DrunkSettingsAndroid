package com.drunkbatya.drunksettings.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.LinkedHashSet

class NotificationManagerServiceLogger : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android" || lpparam.processName != "android") {
            return
        }

        val classNames = listOf(
            "com.android.server.NotificationManagerService",
            "com.android.server.notification.NotificationManagerService"
        )

        val targetClass = classNames.asSequence()
            .mapNotNull { XposedHelpers.findClassIfExists(it, lpparam.classLoader) }
            .firstOrNull()

        if (targetClass == null) {
            XposedBridge.log("DrunkSettings: NotificationManagerService class not found")
            return
        }

        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val member = param.method
                val declaring = member.declaringClass?.name ?: "unknown"
                val argCount = param.args?.size ?: 0
                //XposedBridge.log("DrunkSettings: $declaring#${member.name} args=$argCount")
            }
        }

        val methodNames = LinkedHashSet<String>()
        for (method in targetClass.declaredMethods) {
            methodNames.add(method.name)
        }
        for (name in methodNames) {
            XposedBridge.hookAllMethods(targetClass, name, callback)
        }
        XposedBridge.hookAllConstructors(targetClass, callback)

        XposedBridge.log("DrunkSettings: hooked ${methodNames.size} methods in ${targetClass.name}")
    }
}
