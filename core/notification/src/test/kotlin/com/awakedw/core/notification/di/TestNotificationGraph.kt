package com.awakedw.core.notification.di

import com.awakedw.core.common.AppClock
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.notification.TestFakes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 测试图绑定：:core:notification 的生产依赖里没有 :core:data，
 * 测试组件以假仓储闭环图，接收器经 @AndroidEntryPoint 注入即拿到与断言共享的假体。
 */
@Module
@InstallIn(SingletonComponent::class)
object TestNotificationGraph {
    @Provides
    @Singleton
    fun provideClock(): AppClock = TestFakes.clock

    @Provides
    @Singleton
    fun provideWaterRepository(): WaterRepository = TestFakes.water

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(): UserPreferencesRepository = TestFakes.prefs

    @Provides
    @Singleton
    fun provideCopyLibraryRepository(): CopyLibraryRepository = TestFakes.copies
}
