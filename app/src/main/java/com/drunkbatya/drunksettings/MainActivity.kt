package com.drunkbatya.drunksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.drunkbatya.drunksettings.ui.theme.DrunkSettingsTheme
import com.drunkbatya.drunksettings.ui.SettingsApp
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class MainActivity : ComponentActivity() {
    private var mService: XposedService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                mService = service
                enableEdgeToEdge()
                setContent {
                    DrunkSettingsTheme {
                        SettingsApp(service)
                    }
                }
            }
            override fun onServiceDied(service: XposedService) {
            }
        })

    }
}
