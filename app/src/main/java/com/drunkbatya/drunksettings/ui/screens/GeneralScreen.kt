package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutLabel

@Composable
fun GeneralScreen(
    onBack: () -> Unit,
) {
    val settingsStore = LocalSettingsStore.current
    var showMinSoundDialog by remember { mutableStateOf(false) }
    val generalSeconds = settingsStore.getGeneralMinSoundTimeout()
    SettingsScaffold(title = "General", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = MIN_SOUND_TITLE,
                    summary = timeoutLabel(generalSeconds),
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Timer,
                            contentDescription = null
                        )
                    },
                    onClick = { showMinSoundDialog = true }
                )
            }
        }
    }

    if (showMinSoundDialog) {
        GeneralMinSoundScreen(
            onBack = { showMinSoundDialog = false }
        )
    }
}
