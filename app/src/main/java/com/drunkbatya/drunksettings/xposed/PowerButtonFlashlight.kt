package com.drunkbatya.drunksettings.xposed

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.view.KeyEvent

/**
 * Turns a long-press of the power button, while the screen is off, into a flashlight toggle without
 * waking the screen.
 *
 * PhoneWindowManager wakes the display and runs its power-key gestures inside
 * interceptKeyBeforeQueueing, so the only way to hold the screen off is to swallow the DOWN event
 * before that runs. Because a DOWN cannot be un-swallowed, a release before the long-press threshold
 * is replayed as a normal wake. While enabled, this overrides all screen-off power-key gestures
 * (double-press camera, power menu); the short press still wakes the device.
 */
class PowerButtonFlashlight(
    private val log: (String) -> Unit,
    private val logVerbose: (String) -> Unit,
) {
    private val handlerThread = HandlerThread("DrunkSettings.PowerFlashlight").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private val lock = Any()

    private var tracking = false
    private var consumed = false
    private var longPressRunnable: Runnable? = null

    @Volatile
    private var cameraManager: CameraManager? = null
    @Volatile
    private var torchCameraId: String? = null
    @Volatile
    private var torchEnabled = false
    @Volatile
    private var setupDone = false

    fun onPowerKey(event: KeyEvent, context: Context?): Int? {
        ensureTorchSetup(context)
        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return null
        if (powerManager.isInteractive) {
            return null
        }
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> onDown(event)
            KeyEvent.ACTION_UP -> onUp(event, powerManager)
            else -> null
        }
    }

    private fun onDown(event: KeyEvent): Int? {
        if (event.repeatCount > 0) {
            return synchronized(lock) { if (tracking) CONSUME else null }
        }
        synchronized(lock) {
            cancelPending()
            tracking = true
            consumed = false
            val runnable = Runnable { onLongPressElapsed() }
            longPressRunnable = runnable
            handler.postDelayed(runnable, LONG_PRESS_MS)
        }
        logVerbose("power down swallowed (screen off), waiting for long press")
        return CONSUME
    }

    private fun onLongPressElapsed() {
        synchronized(lock) {
            if (!tracking || consumed) {
                return
            }
            consumed = true
        }
        toggleTorch()
    }

    private fun onUp(event: KeyEvent, powerManager: PowerManager): Int? {
        val wasConsumed: Boolean
        synchronized(lock) {
            if (!tracking) {
                return null
            }
            tracking = false
            cancelPending()
            wasConsumed = consumed
            consumed = false
        }
        if (wasConsumed) {
            logVerbose("power released after torch toggle, screen stays off")
            return CONSUME
        }
        logVerbose("power short press, waking screen")
        wakeUp(powerManager, event.eventTime)
        return CONSUME
    }

    private fun cancelPending() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun toggleTorch() {
        val manager = cameraManager ?: return
        val id = torchCameraId ?: return
        val target = !torchEnabled
        try {
            manager.setTorchMode(id, target)
            torchEnabled = target
            log("power long press, torch toggled to $target")
        } catch (e: Throwable) {
            log("setTorchMode failed for $id: ${e.message}")
        }
    }

    private fun ensureTorchSetup(context: Context?) {
        if (setupDone) {
            return
        }
        val manager = context?.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        setupDone = true
        cameraManager = manager
        torchCameraId = findTorchCamera(manager)
        if (torchCameraId == null) {
            log("no camera with a flash unit found")
            return
        }
        try {
            manager.registerTorchCallback(torchCallback, handler)
        } catch (e: Throwable) {
            log("registerTorchCallback failed: ${e.message}")
        }
    }

    private fun findTorchCamera(manager: CameraManager): String? {
        val ids = try {
            manager.cameraIdList
        } catch (e: Throwable) {
            log("cameraIdList failed: ${e.message}")
            return null
        }
        var fallback: String? = null
        for (id in ids) {
            val characteristics = try {
                manager.getCameraCharacteristics(id)
            } catch (_: Throwable) {
                continue
            }
            if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != true) {
                continue
            }
            if (fallback == null) {
                fallback = id
            }
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return fallback
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                torchEnabled = enabled
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCameraId) {
                torchEnabled = false
            }
        }
    }

    private fun wakeUp(powerManager: PowerManager, eventTime: Long) {
        try {
            val method = PowerManager::class.java.getMethod(
                "wakeUp",
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
            )
            method.invoke(powerManager, eventTime, WAKE_REASON_POWER_BUTTON, WAKE_DETAILS)
            return
        } catch (_: Throwable) {
        }
        try {
            val method = PowerManager::class.java.getMethod("wakeUp", Long::class.javaPrimitiveType)
            method.invoke(powerManager, eventTime)
        } catch (e: Throwable) {
            log("wakeUp failed: ${e.message}")
        }
    }

    companion object {
        private const val CONSUME = 0
        private const val LONG_PRESS_MS = 500L
        private const val WAKE_REASON_POWER_BUTTON = 1
        private const val WAKE_DETAILS = "DrunkSettings:powerShortPress"
    }
}
