package com.drunkbatya.drunksettings.ui.state

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.drunkbatya.drunksettings.data.SettingsStore

@Composable
fun rememberGeneralMinSound(settingsStore: SettingsStore): MutableIntState {
    val prefs = remember(settingsStore) {
        settingsStore.getPrefs()
    }
    val state = remember {
        mutableIntStateOf(
            prefs.getInt(SettingsStore.KEY_GENERAL_MIN_SOUND, SettingsStore.DEFAULT_MIN_SOUND)
        )
    }
    DisposableEffect(SettingsStore.KEY_GENERAL_MIN_SOUND) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == SettingsStore.KEY_GENERAL_MIN_SOUND) {
                state.intValue = prefs.getInt(
                    SettingsStore.KEY_GENERAL_MIN_SOUND,
                    SettingsStore.DEFAULT_MIN_SOUND
                )
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

@Composable
fun rememberAppSpecificTimeout(settingsStore: SettingsStore, packageName: String): MutableState<Int?> {
    val prefs = remember(settingsStore) {
        settingsStore.getPrefs()
    }
    val state = remember(settingsStore, packageName) {
        mutableStateOf(settingsStore.getAppMinSoundTimeout(packageName))
    }
    DisposableEffect(packageName) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == SettingsStore.appKey(packageName)) {
                state.value = settingsStore.getAppMinSoundTimeout(packageName)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
