package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold

@Composable
fun MainScreen(
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLockscreen: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenAbout: () -> Unit
) {
    SettingsScaffold(title = "DrunkSettings") { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Notifications",
                    summary = "Sound limits and per-app rules",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Notifications,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenNotifications
                )
            }
            item {
                SettingsListItem(
                    title = "Privacy",
                    summary = "Headset button, notification & screenshot detection",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Shield,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenPrivacy
                )
            }
            item {
                SettingsListItem(
                    title = "Lockscreen",
                    summary = "Fingerprint and power button behaviour",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Lock,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenLockscreen
                )
            }
            item {
                SettingsListItem(
                    title = "Debug",
                    summary = "To test something",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.BugReport,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenDebug
                )
            }
            item {
                SettingsListItem(
                    title = "About",
                    summary = "App info and links",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Info,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenAbout
                )
            }
        }
    }
}
