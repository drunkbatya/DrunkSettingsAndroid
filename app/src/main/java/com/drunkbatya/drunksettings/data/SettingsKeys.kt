package com.drunkbatya.drunksettings.data

object SettingsKeys {
    const val PREFS_NAME = "NotificationManagerSupervisor"

    const val GENERAL_MIN_SOUND = "general_min_notification_sound_timeout"
    const val GENERAL_DO_NOT_FADE_OUT = "general_do_not_fade_out_music"
    const val DEBUG_ALSO_MUTE_BLINK = "debug_also_mute_blink"
    const val DEBUG_VERBOSE_LOGGING = "debug_verbose_logging"
    const val SWALLOW_HEADSET_BUTTON = "swallow_headset_button"
    const val NOTIF_DETECT_MODE = "notif_detect_mode"
    const val SCREENSHOT_BLOCK = "screenshot_block"
    const val CAPTURE_SECURE_LAYERS = "capture_secure_layers"
    const val FINGERPRINT_SCREEN_ON_ONLY = "fingerprint_screen_on_only"
    const val POWER_TOGGLE_FLASHLIGHT = "power_toggle_flashlight"

    const val APP_MIN_SOUND_PREFIX = "min_notification_sound_timeout_"
    const val NOTIF_DETECT_APP_PREFIX = "notif_detect_mode_"
    const val SCREENSHOT_BLOCK_APP_PREFIX = "screenshot_block_"

    const val DEFAULT_MIN_SOUND = 0

    fun appMinSoundKey(packageName: String) = "$APP_MIN_SOUND_PREFIX$packageName"
    fun notifDetectAppKey(packageName: String) = "$NOTIF_DETECT_APP_PREFIX$packageName"
    fun screenshotBlockAppKey(packageName: String) = "$SCREENSHOT_BLOCK_APP_PREFIX$packageName"
}
