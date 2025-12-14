package com.safe.setting.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun firebase(): InterfaceFirebase
}

class NetworkBatteryReceiver : BroadcastReceiver() {
    companion object { private const val TAG = "NetworkBatteryReceiver" }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val entry = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
            val firebase = entry.firebase()
            val user = firebase.getUser() ?: return
            val childID = user.uid

            val batteryLevel = readBatteryLevel(context, intent)
            val (isOnline, netType) = readConnectivity(context)
            val payload = mapOf(
                "batteryLevel" to batteryLevel,
                "isOnline" to isOnline,
                "networkType" to netType,
                "lastSeen" to System.currentTimeMillis()
            )
            firebase.getDatabaseReference("User_Directory/$childID").updateChildren(payload)
            Log.d(TAG, "User_Directory updated: battery=$batteryLevel online=$isOnline type=$netType")
        } catch (e: Exception) {
            Log.e(TAG, "Receiver update failed: ${e.message}")
        }
    }

    private fun readBatteryLevel(context: Context, intent: Intent): Int {
        return try {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) (level * 100) / scale else -1
            } else {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            }
        } catch (_: Exception) { -1 }
    }

    private fun readConnectivity(context: Context): Pair<Boolean, String> {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false to "No Connection"
            val cap = cm.getNetworkCapabilities(network) ?: return false to "No Connection"
            when {
                cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true to "WIFI"
                cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true to "CELLULAR"
                else -> false to "No Connection"
            }
        } catch (_: Exception) { false to "Unknown" }
    }
}
