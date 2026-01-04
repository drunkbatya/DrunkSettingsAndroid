package com.drunkbatya.drunksettings.ui.model

const val MIN_SOUND_TITLE = "Minimum time between notification sounds"

data class TimeoutOption(val seconds: Int, val label: String)

val timeoutOptions = listOf(
    TimeoutOption(0, "No restrictions"),
    TimeoutOption(10, "10 seconds"),
    TimeoutOption(30, "30 seconds"),
    TimeoutOption(60, "1 minute"),
    TimeoutOption(300, "5 minutes"),
    TimeoutOption(900, "15 minutes"),
    TimeoutOption(1800, "30 minutes")
)

fun timeoutLabel(seconds: Int): String {
    return timeoutOptions.firstOrNull { it.seconds == seconds }?.label ?: "$seconds seconds"
}
