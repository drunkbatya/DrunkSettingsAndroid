package com.drunkbatya.drunksettings.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.util.sendTestNotification

@Composable
fun DebugScreen(
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenSystemFlags: () -> Unit
) {
    SettingsScaffold(title = "Debug", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Notifications",
                    summary = "Send test notifications",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.NotificationsActive,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenNotifications
                )
            }
            item {
                SettingsListItem(
                    title = "System flags",
                    summary = "Live system state",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Speed,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenSystemFlags
                )
            }
            item {
                SettingsListItem(
                    title = "App",
                    summary = "Storage and logging tools",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.BugReport,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenApp
                )
            }
        }
    }
}

@Composable
fun DebugNotificationsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sendTestNotification(context)
        }
    }
    val onTestNotification = {
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sendTestNotification(context)
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SettingsScaffold(title = "Notifications", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Test sound notification",
                    summary = "Post a notification with sound",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null
                        )
                    },
                    onClick = onTestNotification
                )
            }
        }
    }
}

@Composable
fun DebugAppScreen(
    onBack: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    var showDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = "App", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Wipe shared settings",
                    summary = "Remove all saved limits",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.DeleteForever,
                            contentDescription = null
                        )
                    },
                    onClick = { showDialog = true }
                )
            }
            item {
                SettingsListItem(
                    title = "Dump settings to logs",
                    summary = "Write current prefs to logcat",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.AutoMirrored.Rounded.ListAlt,
                            contentDescription = null
                        )
                    },
                    onClick = { settingsStore.dumpToLog() }
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Are you shure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsStore.wipeAll()
                        showDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}
