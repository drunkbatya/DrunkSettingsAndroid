package com.drunkbatya.drunksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.drunkbatya.drunksettings.ui.theme.DrunkSettingsTheme
import com.drunkbatya.drunksettings.ui.SettingsApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrunkSettingsTheme {
                SettingsApp()
            }
        }
    }
}
