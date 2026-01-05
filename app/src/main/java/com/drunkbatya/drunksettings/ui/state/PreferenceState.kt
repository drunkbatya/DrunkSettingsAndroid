package com.drunkbatya.drunksettings.ui.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.drunkbatya.drunksettings.data.SettingsStore
import io.github.libxposed.service.XposedService

@Composable
fun rememberGeneralMinSound(mService: XposedService): MutableIntState {
    val context = LocalContext.current
    val prefs = remember {
        SettingsStore.getPrefs(mService)
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
fun rememberAppSpecificTimeout(mService: XposedService, packageName: String): MutableState<Int?> {
    val context = LocalContext.current
    val prefs = mService.getRemotePreferences(SettingsStore.PREFS_NAME)
    val state = remember { mutableStateOf(SettingsStore.getAppMinSoundTimeout(mService, packageName)) }
    DisposableEffect(packageName) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == SettingsStore.appKey(packageName)) {
                state.value = SettingsStore.getAppMinSoundTimeout(mService, packageName)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
