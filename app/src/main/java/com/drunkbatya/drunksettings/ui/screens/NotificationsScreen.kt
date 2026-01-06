package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenApps: () -> Unit
) {
    SettingsScaffold(title = "Notifications", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "General",
                    summary = "Base settings",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Tune,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenGeneral
                )
            }
            item {
                SettingsListItem(
                    title = "App notifications",
                    summary = "Per-app settings",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Apps,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenApps
                )
            }
        }
    }
}
