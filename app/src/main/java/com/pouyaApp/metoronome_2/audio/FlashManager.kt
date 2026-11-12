package com.pouyaApp.metoronome_2.audio

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

class FlashManager(context: Context) {

    private val cameraManager =
        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val flashing = AtomicBoolean(false)
    private var torchCameraId: String? = null

    val isAvailable: Boolean
        get() = torchCameraId != null

    init {
        torchCameraId = findTorchCameraId()
    }

    fun pulse(durationMs: Long = 60L) {
        val cameraId = torchCameraId ?: return
        if (!flashing.compareAndSet(false, true)) return
        try {
            cameraManager.setTorchMode(cameraId, true)
            mainHandler.postDelayed({
                try {
                    cameraManager.setTorchMode(cameraId, false)
                } catch (_: CameraAccessException) {
                } finally {
                    flashing.set(false)
                }
            }, durationMs)
        } catch (_: CameraAccessException) {
            flashing.set(false)
        }
    }

    fun release() {
        torchCameraId?.let { id ->
            try {
                cameraManager.setTorchMode(id, false)
            } catch (_: CameraAccessException) {
            }
        }
    }

    private fun findTorchCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: CameraAccessException) {
            null
        }
    }
}
