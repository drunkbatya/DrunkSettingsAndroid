package com.drunkbatya.drunksettings.ui.model

const val HEADSET_SWALLOW_TITLE = "Ignore wired headset button"
const val NOTIF_DETECT_TITLE = "Notification-status visibility"
const val SCREENSHOT_BLOCK_TITLE = "Hide screenshots from apps"

/**
 * How the system answers an app that asks whether its notifications are enabled
 * (areNotificationsEnabled / NotificationChannel importance).
 * Stored as [name] in preferences and mirrored in the Xposed runtime.
 */
enum class NotifDetectMode(val storageValue: String, val label: String) {
    DIRECT("direct", "Direct (report real state)"),
    FAKE_ON("fake_on", "Fake on (always report enabled)"),
    FAKE_OFF("fake_off", "Fake off (always report disabled)");

    companion object {
        val DEFAULT = DIRECT

        fun fromStorage(value: String?): NotifDetectMode {
            return entries.firstOrNull { it.storageValue == value } ?: DEFAULT
        }
    }
}

fun notifDetectLabel(mode: NotifDetectMode): String = mode.label
