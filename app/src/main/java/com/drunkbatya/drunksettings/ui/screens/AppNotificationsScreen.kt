package com.drunkbatya.drunksettings.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import com.drunkbatya.drunksettings.ui.components.AppIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import com.drunkbatya.drunksettings.ui.data.rememberInstalledApps
import com.drunkbatya.drunksettings.ui.model.AppInfo

@Composable
fun AppNotificationsScreen(
    onBack: () -> Unit,
    onOpenApp: (AppInfo) -> Unit
) {
    val apps = rememberInstalledApps()
    var showSystemApps by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = "App notifications",
        onBack = onBack,
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Menu"
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Show system apps") },
                    onClick = {
                        showSystemApps = !showSystemApps
                        menuExpanded = false
                    },
                    trailingIcon = {
                        Checkbox(
                            checked = showSystemApps,
                            onCheckedChange = null
                        )
                    }
                )
            }
        }
    ) { padding ->
        if (apps.value == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@SettingsScaffold
        }

        val visibleApps = apps.value.orEmpty().filter { showSystemApps || it.isUserApp }
        LazyColumn(contentPadding = padding) {
            items(visibleApps, key = { it.packageName }) { app ->
                SettingsListItem(
                    title = app.label,
                    summary = app.packageName,
                    onClick = { onOpenApp(app) },
                    leadingContent = { AppIcon(app) }
                )
            }
        }
    }
}
