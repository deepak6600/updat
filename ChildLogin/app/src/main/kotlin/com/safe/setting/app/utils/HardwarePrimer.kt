package com.safe.setting.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class HardwarePrimer(private val context: Context) {

    companion object {
        private const val TAG = "HardwarePrimer"
    }

    fun primeCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted. Skipping priming.")
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraThread = HandlerThread("CameraPrimerThread").apply { start() }
        val cameraHandler = Handler(cameraThread.looper)

        try {
            val cameraId = cameraManager.cameraIdList[0]
            Log.d(TAG, "Priming camera: $cameraId")

            val stateCallback = object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera primed successfully. Closing.")
                    camera.close()
                    cameraThread.quitSafely()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected during priming.")
                    camera.close()
                    cameraThread.quitSafely()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Error priming camera: $error")
                    camera.close()
                    cameraThread.quitSafely()
                }
            }
            cameraManager.openCamera(cameraId, stateCallback, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera priming: ${e.message}")
            cameraThread.quitSafely()
        }
    }

    fun primeMicrophone() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Record audio permission not granted. Skipping priming.")
            return
        }

        Log.d(TAG, "Priming microphone...")
        var recorder: MediaRecorder? = null
        val dummyFile = File(context.cacheDir, "primer.tmp")

        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                setOutputFile(dummyFile.absolutePath)
                prepare()
                start()
            }

            // ===============================================================
            // ### NAYA SURAKSHA CODE ###
            // ===============================================================
            // ### HINDI COMMENT ###
            // Hum yahan MediaRecorder ko stop aur release karne se pehle thoda intezaar karte hain.
            // Kabhi-kabhi, turant stop karne se systems (khaaskar MIUI) mein samasya aa sakti hai.
            Handler(HandlerThread("PrimerStopper").apply { start() }.looper).postDelayed({
                try {
                    recorder.stop()
                    Log.d(TAG, "Microphone primed successfully.")
                } catch (e: Exception) {
                    // ### LOGIC ###
                    // Yeh 'catch' block akele hi sabse zaroori hai.
                    // Agar OS connection kaat bhi de (jisse stop() fail ho),
                    // hamara app crash nahi hoga. Hum is error ko chupchaap handle kar lenge.
                    Log.e(TAG, "MediaRecorder stop failed (handled gracefully): ${e.message}")
                } finally {
                    recorder.release()
                    if (dummyFile.exists()) {
                        dummyFile.delete()
                    }
                }
            }, 100) // 100 millisecond ka chota sa delay

        } catch (e: IOException) {
            Log.e(TAG, "Failed to prime microphone (prepare/start): ${e.message}")
            recorder?.release()
            if (dummyFile.exists()) {
                dummyFile.delete()
            }
        }
    }
}