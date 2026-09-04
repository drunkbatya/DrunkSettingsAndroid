package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold

@Composable
fun LockscreenScreen(
    onBack: () -> Unit,
) {
    val settingsStore = LocalSettingsStore.current
    var fingerprintScreenOnOnly by remember {
        mutableStateOf(settingsStore.getFingerprintScreenOnOnly())
    }
    var powerToggleFlashlight by remember {
        mutableStateOf(settingsStore.getPowerToggleFlashlight())
    }
    SettingsScaffold(title = "Lockscreen", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Fingerprint only when screen on",
                    summary = "Ignore the fingerprint sensor while the screen is off",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.Fingerprint, contentDescription = null)
                    },
                    trailingContent = {
                        Checkbox(checked = fingerprintScreenOnOnly, onCheckedChange = null)
                    },
                    onClick = {
                        fingerprintScreenOnOnly = !fingerprintScreenOnOnly
                        settingsStore.setFingerprintScreenOnOnly(fingerprintScreenOnOnly)
                    }
                )
            }
            item {
                SettingsListItem(
                    title = "Power button flashlight",
                    summary = "Long-press power while the screen is off to toggle the torch",
                    leadingContent = {
                        SettingsIcon(icon = Icons.Rounded.FlashlightOn, contentDescription = null)
                    },
                    trailingContent = {
                        Checkbox(checked = powerToggleFlashlight, onCheckedChange = null)
                    },
                    onClick = {
                        powerToggleFlashlight = !powerToggleFlashlight
                        settingsStore.setPowerToggleFlashlight(powerToggleFlashlight)
                    }
                )
            }
        }
    }
}
