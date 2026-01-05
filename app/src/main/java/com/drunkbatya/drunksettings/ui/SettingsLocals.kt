package com.drunkbatya.drunksettings.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.drunkbatya.drunksettings.data.SettingsStore

val LocalSettingsStore = staticCompositionLocalOf<SettingsStore> {
    error("SettingsStore not provided")
}
