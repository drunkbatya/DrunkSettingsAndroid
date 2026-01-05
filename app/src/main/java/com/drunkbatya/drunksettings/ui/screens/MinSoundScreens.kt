package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.drunkbatya.drunksettings.data.SettingsStore
import com.drunkbatya.drunksettings.ui.components.OptionListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutOptions
import com.drunkbatya.drunksettings.ui.state.rememberAppSpecificTimeout
import com.drunkbatya.drunksettings.ui.state.rememberGeneralMinSound
import io.github.libxposed.service.XposedService

@Composable
fun GeneralMinSoundScreen(
    mService: XposedService,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val selected = rememberGeneralMinSound(mService)
    MinSoundOptionsScreen(
        selectedSeconds = selected.intValue,
        onBack = onBack,
        includeNotSet = false,
        onSelect = { seconds ->
            selected.intValue = seconds
            SettingsStore.setGeneralMinSoundTimeout(mService, seconds)
        },
        onClear = {}
    )
}

@Composable
fun AppMinSoundScreen(
    mService: XposedService,
    packageName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val selected = rememberAppSpecificTimeout(mService, packageName)
    MinSoundOptionsScreen(
        selectedSeconds = selected.value,
        onBack = onBack,
        includeNotSet = true,
        onSelect = { seconds ->
            selected.value = seconds
            SettingsStore.setAppMinSoundTimeout(mService, packageName, seconds)
        },
        onClear = {
            selected.value = null
            SettingsStore.clearAppMinSoundTimeout(mService, packageName)
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
