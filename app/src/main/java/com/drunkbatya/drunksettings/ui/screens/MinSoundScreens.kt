package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.model.MIN_SOUND_TITLE
import com.drunkbatya.drunksettings.ui.model.timeoutOptions

@Composable
fun GeneralMinSoundScreen(
    onBack: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    val selected = settingsStore.getGeneralMinSoundTimeout()
    MinSoundOptionsDialog(
        selectedSeconds = selected,
        includeNotSet = false,
        onSelect = { seconds ->
            settingsStore.setGeneralMinSoundTimeout(seconds)
            onBack()
        },
        onClear = {},
        onDismiss = onBack
    )
}

@Composable
fun AppMinSoundScreen(
    packageName: String,
    onBack: () -> Unit
) {
    val settingsStore = LocalSettingsStore.current
    val selected = settingsStore.getAppMinSoundTimeout(packageName)
    MinSoundOptionsDialog(
        selectedSeconds = selected,
        includeNotSet = true,
        onSelect = { seconds ->
            settingsStore.setAppMinSoundTimeout(packageName, seconds)
            onBack()
        },
        onClear = {
            settingsStore.clearAppMinSoundTimeout(packageName)
            onBack()
        },
        onDismiss = onBack
    )
}

@Composable
fun MinSoundOptionsDialog(
    selectedSeconds: Int?,
    includeNotSet: Boolean,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(MIN_SOUND_TITLE) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                if (includeNotSet) {
                    item(key = "not_set") {
                        RadioOptionRow(
                            label = "Not set",
                            selected = selectedSeconds == null,
                            onClick = onClear
                        )
                    }
                }
                items(timeoutOptions, key = { it.seconds }) { option ->
                    RadioOptionRow(
                        label = option.label,
                        selected = selectedSeconds == option.seconds,
                        onClick = { onSelect(option.seconds) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RadioOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
