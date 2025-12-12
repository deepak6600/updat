package com.safe.setting.app.di.module

import com.safe.setting.app.ui.activities.login.InteractorLogin
import com.safe.setting.app.ui.activities.login.InterfaceInteractorLogin
import com.safe.setting.app.ui.activities.login.InterfaceViewLogin
import com.safe.setting.app.ui.activities.register.InteractorRegister
import com.safe.setting.app.ui.activities.register.InterfaceInteractorRegister
import com.safe.setting.app.ui.activities.register.InterfaceViewRegister
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class ActivityInteractorsModule {

    @Binds
    abstract fun bindLoginInteractor(impl: InteractorLogin<InterfaceViewLogin>): InterfaceInteractorLogin<InterfaceViewLogin>

    @Binds
    abstract fun bindRegisterInteractor(impl: InteractorRegister<InterfaceViewRegister>): InterfaceInteractorRegister<InterfaceViewRegister>
}
