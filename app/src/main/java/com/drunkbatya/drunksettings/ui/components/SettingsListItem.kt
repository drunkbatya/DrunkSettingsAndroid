package com.drunkbatya.drunksettings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

@Composable
fun SettingsListItem(
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }
    val textStartPadding = if (leadingContent != null) 8.dp else 0.dp
    @Suppress("DEPRECATION")
    val chevronIcon = Icons.Rounded.KeyboardArrowRight

    Row(
        modifier = modifier
            .heightIn(min = 72.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingContent()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = textStartPadding, end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when {
            trailingContent != null -> trailingContent()
            showChevron -> Icon(
                imageVector = chevronIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}

@Composable
fun OptionListItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SettingsListItem(
        title = label,
        onClick = onClick,
        trailingContent = {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        showChevron = false
    )
}

@Composable
fun SettingsIcon(
    icon: ImageVector,
    contentDescription: String?,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = Modifier
            .size(containerSize)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
