package com.safe.setting.app.di.module

import com.safe.setting.app.services.call.InteractorCall
import com.safe.setting.app.services.call.InterfaceInteractorCall
import com.safe.setting.app.services.call.InterfaceServiceCall
import com.safe.setting.app.services.devicestatus.InteractorDeviceStatus
import com.safe.setting.app.services.devicestatus.InterfaceInteractorDeviceStatus
import com.safe.setting.app.services.devicestatus.InterfaceServiceDeviceStatus
import com.safe.setting.app.services.sms.InteractorSms
import com.safe.setting.app.services.sms.InterfaceInteractorSms
import com.safe.setting.app.services.sms.InterfaceServiceSms
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@Module
@InstallIn(ServiceComponent::class)
abstract class ServiceInteractorsModule {

    @Binds
    abstract fun bindCallInteractor(impl: InteractorCall<InterfaceServiceCall>): InterfaceInteractorCall<InterfaceServiceCall>

    @Binds
    abstract fun bindDeviceStatusInteractor(impl: InteractorDeviceStatus<InterfaceServiceDeviceStatus>): InterfaceInteractorDeviceStatus<InterfaceServiceDeviceStatus>

    @Binds
    abstract fun bindSmsInteractor(impl: InteractorSms<InterfaceServiceSms>): InterfaceInteractorSms<InterfaceServiceSms>
}
