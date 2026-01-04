package com.drunkbatya.drunksettings.ui.model

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val label: String,
    val isUserApp: Boolean,
    val icon: ImageBitmap?
)
