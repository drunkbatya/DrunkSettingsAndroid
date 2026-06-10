package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.NOTIF_DETECT_TITLE
import com.drunkbatya.drunksettings.ui.model.SCREENSHOT_BLOCK_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutLabel

@Composable
fun AppDetailScreen(
    appLabel: String,
    appPackage: String,
    onBack: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    var showMinSoundDialog by remember { mutableStateOf(false) }
    var showNotifDialog by remember { mutableStateOf(false) }
    var showScreenshotDialog by remember { mutableStateOf(false) }

    val currentSeconds = settingsStore.getAppMinSoundTimeout(appPackage)
    var notifMode by remember { mutableStateOf(settingsStore.getAppNotifDetectMode(appPackage)) }
    var screenshotBlock by remember { mutableStateOf(settingsStore.getAppScreenshotBlock(appPackage)) }

    SettingsScaffold(title = appLabel, onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = MIN_SOUND_TITLE,
                    summary = currentSeconds?.let { timeoutLabel(it) } ?: "Not set",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.Timer, contentDescription = null)
                    },
                    onClick = { showMinSoundDialog = true }
                )
            }
            item {
                SettingsListItem(
                    title = NOTIF_DETECT_TITLE,
                    summary = notifMode?.label ?: "Inherit (global)",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.NotificationsActive, contentDescription = null)
                    },
                    onClick = { showNotifDialog = true }
                )
            }
            item {
                SettingsListItem(
                    title = SCREENSHOT_BLOCK_TITLE,
                    summary = when (screenshotBlock) {
                        true -> "On"
                        false -> "Off"
                        null -> "Inherit (global)"
                    },
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.Screenshot, contentDescription = null)
                    },
                    onClick = { showScreenshotDialog = true }
                )
            }
        }
    }

    if (showMinSoundDialog) {
        AppMinSoundScreen(
            packageName = appPackage,
            onBack = { showMinSoundDialog = false }
        )
    }

    if (showNotifDialog) {
        NotifDetectModeDialog(
            selected = notifMode,
            includeInherit = true,
            onSelect = { mode ->
                notifMode = mode
                settingsStore.setAppNotifDetectMode(appPackage, mode)
                showNotifDialog = false
            },
            onInherit = {
                notifMode = null
                settingsStore.clearAppNotifDetectMode(appPackage)
                showNotifDialog = false
            },
            onDismiss = { showNotifDialog = false }
        )
    }

    if (showScreenshotDialog) {
        TriStateDialog(
            title = SCREENSHOT_BLOCK_TITLE,
            selected = screenshotBlock,
            onSelect = { enabled ->
                screenshotBlock = enabled
                settingsStore.setAppScreenshotBlock(appPackage, enabled)
                showScreenshotDialog = false
            },
            onInherit = {
                screenshotBlock = null
                settingsStore.clearAppScreenshotBlock(appPackage)
                showScreenshotDialog = false
            },
            onDismiss = { showScreenshotDialog = false }
        )
    }
}
