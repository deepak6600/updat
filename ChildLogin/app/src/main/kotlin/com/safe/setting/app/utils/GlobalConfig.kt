package com.safe.setting.app.utils

import android.content.Context
import android.util.Log

object GlobalConfig {
    private const val TAG = "GlobalConfig"
    private const val PREFS_NAME = "global_config"
    private const val KEY_FROZEN = "isFrozen"

    @Volatile
    var isFrozen: Boolean = false
        private set

    fun init(context: Context) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isFrozen = prefs.getBoolean(KEY_FROZEN, false)
            Log.d(TAG, "Initialized isFrozen=$isFrozen")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed: ${e.message}")
        }
    }

    fun setFrozen(context: Context, frozen: Boolean) {
        try {
            isFrozen = frozen
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FROZEN, frozen).apply()
            Log.d(TAG, "isFrozen set to $frozen")
        } catch (e: Exception) {
            Log.e(TAG, "Persist failed: ${e.message}")
        }
    }
}
