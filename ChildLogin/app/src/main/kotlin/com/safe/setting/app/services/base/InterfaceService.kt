package com.safe.setting.app.services.base

import io.reactivex.rxjava3.disposables.Disposable

interface InterfaceService {

    fun addDisposable(disposable: Disposable)

    fun clearDisposable()

}