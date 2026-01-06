package com.drunkbatya.drunksettings.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold

@Composable
fun DebugSystemFlagsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var flags by remember { mutableStateOf(readSystemFlags(context)) }

    SettingsScaffold(
        title = "System flags",
        onBack = onBack,
        actions = {
            IconButton(onClick = { flags = readSystemFlags(context) }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "AudioManager.isMusicActive",
                    summary = flags.isMusicActive.toString(),
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

private data class SystemFlags(
    val isMusicActive: Boolean
)

private fun readSystemFlags(context: Context): SystemFlags {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return SystemFlags(isMusicActive = audioManager.isMusicActive)
}
