package com.drunkbatya.drunksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.drunkbatya.drunksettings.data.SettingsStore
import com.drunkbatya.drunksettings.ui.theme.DrunkSettingsTheme
import com.drunkbatya.drunksettings.ui.LocalSettingsStore
import com.drunkbatya.drunksettings.ui.SettingsApp
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                val settingsStore = SettingsStore(service)
                enableEdgeToEdge()
                setContent {
                    DrunkSettingsTheme {
                        CompositionLocalProvider(LocalSettingsStore provides settingsStore) {
                            SettingsApp()
                        }
                    }
                }
            }
            override fun onServiceDied(service: XposedService) {
            }
        })

    }
}
