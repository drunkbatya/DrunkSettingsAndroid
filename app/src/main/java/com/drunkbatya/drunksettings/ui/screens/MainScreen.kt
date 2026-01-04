package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
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
                    onClick = onOpenNotifications
                )
            }
            item {
                SettingsListItem(
                    title = "Debug",
                    onClick = onOpenDebug
                )
            }
        }
    }
}
