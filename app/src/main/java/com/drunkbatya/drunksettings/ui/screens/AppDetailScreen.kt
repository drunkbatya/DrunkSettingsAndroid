package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutLabel
import com.drunkbatya.drunksettings.ui.state.rememberAppSpecificTimeout

@Composable
fun AppDetailScreen(
    appLabel: String,
    appPackage: String,
    onBack: () -> Unit,
    onOpenMinSound: () -> Unit
) {
    val currentSeconds = rememberAppSpecificTimeout(appPackage).value
    SettingsScaffold(title = appLabel, onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = MIN_SOUND_TITLE,
                    summary = currentSeconds?.let { timeoutLabel(it) } ?: "Not set",
                    onClick = onOpenMinSound
                )
            }
        }
    }
}
