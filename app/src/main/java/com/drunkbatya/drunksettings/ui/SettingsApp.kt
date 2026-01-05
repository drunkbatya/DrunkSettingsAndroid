package com.drunkbatya.drunksettings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.drunkbatya.drunksettings.ui.screens.AppDetailScreen
import com.drunkbatya.drunksettings.ui.screens.AppMinSoundScreen
import com.drunkbatya.drunksettings.ui.screens.AppNotificationsScreen
import com.drunkbatya.drunksettings.ui.screens.DebugAppScreen
import com.drunkbatya.drunksettings.ui.screens.DebugNotificationsScreen
import com.drunkbatya.drunksettings.ui.screens.DebugScreen
import com.drunkbatya.drunksettings.ui.screens.GeneralMinSoundScreen
import com.drunkbatya.drunksettings.ui.screens.GeneralScreen
import com.drunkbatya.drunksettings.ui.screens.MainScreen
import com.drunkbatya.drunksettings.ui.screens.NotificationsScreen

@Composable
fun SettingsApp() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Main) }
    val current = backStack.last()
    val onBack = rememberUpdatedState {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        onBack.value.invoke()
    }

    when (current) {
        Screen.Main -> MainScreen(
            onOpenNotifications = { backStack.add(Screen.Notifications) },
            onOpenDebug = { backStack.add(Screen.Debug) }
        )
        Screen.Notifications -> NotificationsScreen(
            onBack = { onBack.value.invoke() },
            onOpenGeneral = { backStack.add(Screen.General) },
            onOpenApps = { backStack.add(Screen.AppNotifications) }
        )
        Screen.Debug -> DebugScreen(
            onBack = { onBack.value.invoke() },
            onOpenNotifications = { backStack.add(Screen.DebugNotifications) },
            onOpenApp = { backStack.add(Screen.DebugApp) }
        )
        Screen.General -> GeneralScreen(
            onBack = { onBack.value.invoke() },
            onOpenMinSound = { backStack.add(Screen.GeneralMinSound) }
        )
        Screen.GeneralMinSound -> {
            GeneralScreen(
                onBack = { onBack.value.invoke() },
                onOpenMinSound = {}
            )
            GeneralMinSoundScreen(
                onBack = { onBack.value.invoke() }
            )
        }
        Screen.AppNotifications -> AppNotificationsScreen(
            onBack = { onBack.value.invoke() },
            onOpenApp = { app ->
                backStack.add(Screen.AppDetail(app.packageName, app.label))
            }
        )
        Screen.DebugNotifications -> DebugNotificationsScreen(
            onBack = { onBack.value.invoke() }
        )
        Screen.DebugApp -> DebugAppScreen(
            onBack = { onBack.value.invoke() }
        )
        is Screen.AppDetail -> {
            val app = current
            AppDetailScreen(
                appLabel = app.label,
                appPackage = app.packageName,
                onBack = { onBack.value.invoke() },
                onOpenMinSound = {
                    backStack.add(Screen.AppMinSound(app.packageName, app.label))
                }
            )
        }
        is Screen.AppMinSound -> {
            val app = current
            AppDetailScreen(
                appLabel = app.label,
                appPackage = app.packageName,
                onBack = { onBack.value.invoke() },
                onOpenMinSound = {}
            )
            AppMinSoundScreen(
                packageName = app.packageName,
                onBack = { onBack.value.invoke() }
            )
        }
    }
}

sealed interface Screen {
    data object Main : Screen
    data object Notifications : Screen
    data object Debug : Screen
    data object DebugNotifications : Screen
    data object DebugApp : Screen
    data object General : Screen
    data object GeneralMinSound : Screen
    data object AppNotifications : Screen
    data class AppDetail(val packageName: String, val label: String) : Screen
    data class AppMinSound(val packageName: String, val label: String) : Screen
}
