package com.safe.setting.app.services.base

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseService : Service(), InterfaceService {

    protected lateinit var serviceScope: CoroutineScope

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }


}