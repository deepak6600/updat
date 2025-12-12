package com.safe.setting.app.services.base

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class BaseService : Service(), InterfaceService {

    private lateinit var compositeDisposable: CompositeDisposable

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        compositeDisposable = CompositeDisposable()
    }

    override fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    override fun clearDisposable() = compositeDisposable.clear()

}