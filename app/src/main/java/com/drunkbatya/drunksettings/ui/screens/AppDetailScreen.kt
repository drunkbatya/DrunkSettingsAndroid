package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutLabel

@Composable
fun AppDetailScreen(
    appLabel: String,
    appPackage: String,
    onBack: () -> Unit,
    onOpenMinSound: () -> Unit,
) {
    val settingsStore = LocalSettingsStore.current
    val currentSeconds = settingsStore.getAppMinSoundTimeout(appPackage)
    SettingsScaffold(title = appLabel, onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = MIN_SOUND_TITLE,
                    summary = currentSeconds?.let { timeoutLabel(it) } ?: "Not set",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Timer,
                            contentDescription = null
                        )
                    },
                    onClick = onOpenMinSound
                )
            }
        }
    }
}
