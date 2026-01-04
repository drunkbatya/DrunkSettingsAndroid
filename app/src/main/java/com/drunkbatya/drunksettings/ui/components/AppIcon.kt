package com.drunkbatya.drunksettings.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drunkbatya.drunksettings.ui.model.AppInfo
import java.util.Locale

@Composable
fun AppIcon(app: AppInfo) {
    val icon = app.icon
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = app.label,
            modifier = Modifier.size(40.dp)
        )
        return
    }
    val color = remember(app.packageName) { avatarColor(app.packageName) }
    val initial = remember(app.label) {
        val trimmed = app.label.trim()
        if (trimmed.isEmpty()) "?" else trimmed.substring(0, 1).uppercase(Locale.getDefault())
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun avatarColor(seed: String): Color {
    val hash = seed.hashCode()
    val hue = (hash and 0xFF) / 255f * 360f
    return Color.hsv(hue, 0.35f, 0.85f)
}
