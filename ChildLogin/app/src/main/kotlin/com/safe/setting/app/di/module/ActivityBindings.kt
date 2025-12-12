package com.safe.setting.app.di.module

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object ActivityBindings {
    @Provides
    fun provideFragmentManager(activity: FragmentActivity): FragmentManager = activity.supportFragmentManager
}
