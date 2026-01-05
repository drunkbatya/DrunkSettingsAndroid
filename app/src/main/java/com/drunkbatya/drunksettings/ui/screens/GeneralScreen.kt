package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutLabel

@Composable
fun GeneralScreen(
    onBack: () -> Unit,
    onOpenMinSound: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    val generalSeconds = settingsStore.getGeneralMinSoundTimeout()
    SettingsScaffold(title = "General", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = MIN_SOUND_TITLE,
                    summary = timeoutLabel(generalSeconds),
                    onClick = onOpenMinSound
                )
            }
        }
    }
}
