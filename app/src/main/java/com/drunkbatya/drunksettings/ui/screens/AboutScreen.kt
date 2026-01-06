package com.drunkbatya.drunksettings.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drunkbatya.drunksettings.ui.components.SettingsIcon
import com.drunkbatya.drunksettings.ui.components.SettingsListItem
import com.drunkbatya.drunksettings.ui.components.SettingsScaffold
import androidx.core.net.toUri

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val version = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            ).versionName
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
    }.getOrNull() ?: "Unknown"
    val whyText = (
        "Modern phones and their OS are fucking awful and horrible. " +
            "Bloated, noisy, manipulative bullshit that keeps stealing your attention. " +
            "Every update adds more dumb friction and more useless junk I never asked for. " +
            "I fucking hate this garbage, and yeah, I fucked their mouths. " +
            "This app is my attempt to add the basic attention-saving stuff " +
            "Android should have had by design."
        )
    SettingsScaffold(title = "About", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SectionHeader(title = "Why?")
            }
            item {
                SectionTextBlock(text = whyText)
            }
            item {
                SectionHeader(title = "Info")
            }
            item {
                SettingsListItem(
                    title = "Sources",
                    summary = "GitHub link",
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Code,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/drunkbatya/DrunkSettingsAndroid".toUri()
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                )
            }
            item {
                SettingsListItem(
                    title = "Version",
                    summary = version,
                    leadingContent = {
                        SettingsIcon(
                            icon = Icons.Rounded.Info,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionTextBlock(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )
    SectionDivider()
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}
