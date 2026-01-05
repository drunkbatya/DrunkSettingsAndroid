package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.drunkbatya.drunksettings.ui.components.OptionListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutOptions
import com.drunkbatya.drunksettings.ui.LocalSettingsStore

@Composable
fun GeneralMinSoundScreen(
    onBack: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    var selected by remember {
        mutableIntStateOf(settingsStore.getGeneralMinSoundTimeout())
    }
    MinSoundOptionsScreen(
        selectedSeconds = selected,
        onBack = onBack,
        includeNotSet = false,
        onSelect = { seconds ->
            selected = seconds
            settingsStore.setGeneralMinSoundTimeout(seconds)
        },
        onClear = {}
    )
}

@Composable
fun AppMinSoundScreen(
    packageName: String,
    onBack: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    var selected by remember(packageName) {
        mutableStateOf(settingsStore.getAppMinSoundTimeout(packageName))
    }
    MinSoundOptionsScreen(
        selectedSeconds = selected,
        onBack = onBack,
        includeNotSet = true,
        onSelect = { seconds ->
            selected = seconds
            settingsStore.setAppMinSoundTimeout(packageName, seconds)
        },
        onClear = {
            selected = null
            settingsStore.clearAppMinSoundTimeout(packageName)
        }
    )
}

@Composable
fun MinSoundOptionsScreen(
    selectedSeconds: Int?,
    onBack: () -> Unit,
    includeNotSet: Boolean,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit
) {
    SettingsScaffold(title = MIN_SOUND_TITLE, onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            if (includeNotSet) {
                item(key = "not_set") {
                    OptionListItem(
                        label = "Not set",
                        selected = selectedSeconds == null,
                        onClick = onClear
                    )
                }
            }
            items(timeoutOptions, key = { it.seconds }) { option ->
                OptionListItem(
                    label = option.label,
                    selected = selectedSeconds == option.seconds,
                    onClick = { onSelect(option.seconds) }
                )
            }
        }
    }
}
