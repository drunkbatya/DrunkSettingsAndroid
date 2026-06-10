package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.model.HEADSET_SWALLOW_TITLE
import com.drunkbatya.drunksettings.ui.model.NOTIF_DETECT_TITLE
import com.drunkbatya.drunksettings.ui.model.NotifDetectMode
import com.drunkbatya.drunksettings.ui.model.SCREENSHOT_BLOCK_TITLE

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onOpenApps: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    var swallowHeadset by remember { mutableStateOf(settingsStore.getSwallowHeadsetButton()) }
    var screenshotBlock by remember { mutableStateOf(settingsStore.getGeneralScreenshotBlock()) }
    var captureSecure by remember { mutableStateOf(settingsStore.getCaptureSecureLayers()) }
    var notifMode by remember { mutableStateOf(settingsStore.getGeneralNotifDetectMode()) }
    var showNotifDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = "Privacy", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = HEADSET_SWALLOW_TITLE,
                    summary = "Swallow wired headset button presses",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.Headset, contentDescription = null)
                    },
                    trailingContent = {
                        Checkbox(checked = swallowHeadset, onCheckedChange = null)
                    },
                    onClick = {
                        swallowHeadset = !swallowHeadset
                        settingsStore.setSwallowHeadsetButton(swallowHeadset)
                    }
                )
            }
            item {
                SettingsListItem(
                    title = NOTIF_DETECT_TITLE,
                    summary = notifMode.label,
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.NotificationsActive, contentDescription = null)
                    },
                    onClick = { showNotifDialog = true }
                )
            }
            item {
                SettingsListItem(
                    title = SCREENSHOT_BLOCK_TITLE,
                    summary = "Hide screenshot events from apps",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.Screenshot, contentDescription = null)
                    },
                    trailingContent = {
                        Checkbox(checked = screenshotBlock, onCheckedChange = null)
                    },
                    onClick = {
                        screenshotBlock = !screenshotBlock
                        settingsStore.setGeneralScreenshotBlock(screenshotBlock)
                    }
                )
            }
            item {
                SettingsListItem(
                    title = "Force-allow screenshots",
                    summary = "Capture FLAG_SECURE content (stories, secure apps)",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.NoPhotography, contentDescription = null)
                    },
                    trailingContent = {
                        Checkbox(checked = captureSecure, onCheckedChange = null)
                    },
                    onClick = {
                        captureSecure = !captureSecure
                        settingsStore.setCaptureSecureLayers(captureSecure)
                    }
                )
            }
            item {
                SettingsListItem(
                    title = "Per-app overrides",
                    summary = "Override the rules above per application",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.Apps, contentDescription = null)
                    },
                    onClick = onOpenApps,
                    showChevron = true
                )
            }
        }
    }

    if (showNotifDialog) {
        NotifDetectModeDialog(
            selected = notifMode,
            includeInherit = false,
            onSelect = { mode ->
                notifMode = mode
                settingsStore.setGeneralNotifDetectMode(mode)
                showNotifDialog = false
            },
            onInherit = {},
            onDismiss = { showNotifDialog = false }
        )
    }
}

@Composable
fun NotifDetectModeDialog(
    selected: NotifDetectMode?,
    includeInherit: Boolean,
    onSelect: (NotifDetectMode) -> Unit,
    onInherit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(NOTIF_DETECT_TITLE) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (includeInherit) {
                    item(key = "inherit") {
                        RadioOptionRow(
                            label = "Inherit (global)",
                            selected = selected == null,
                            onClick = onInherit
                        )
                    }
                }
                items(NotifDetectMode.entries) { mode ->
                    RadioOptionRow(
                        label = mode.label,
                        selected = selected == mode,
                        onClick = { onSelect(mode) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TriStateDialog(
    title: String,
    selected: Boolean?,
    onSelect: (Boolean) -> Unit,
    onInherit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = "inherit") {
                    RadioOptionRow(
                        label = "Inherit (global)",
                        selected = selected == null,
                        onClick = onInherit
                    )
                }
                item(key = "on") {
                    RadioOptionRow(
                        label = "On",
                        selected = selected == true,
                        onClick = { onSelect(true) }
                    )
                }
                item(key = "off") {
                    RadioOptionRow(
                        label = "Off",
                        selected = selected == false,
                        onClick = { onSelect(false) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RadioOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
