package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
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
                    onClick = onOpenGeneral
                )
            }
            item {
                SettingsListItem(
                    title = "App notifications",
                    onClick = onOpenApps
                )
            }
        }
    }
}
