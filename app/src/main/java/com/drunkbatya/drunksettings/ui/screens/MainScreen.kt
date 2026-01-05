package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold

@Composable
fun MainScreen(
    onOpenNotifications: () -> Unit,
    onOpenDebug: () -> Unit
) {
    SettingsScaffold(title = "Settings") { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Notifications",
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
                    title = "Debug",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.BugReport,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenDebug
                )
            }
        }
    }
}
