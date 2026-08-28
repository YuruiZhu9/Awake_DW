package com.awakedw.core.notification.di

import com.awakedw.core.common.AppClock
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.notification.ReminderScheduler
import com.awakedw.core.notification.ReminderSchedulerImpl
import com.awakedw.core.notification.TestFakes
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * 测试图替换：:core:notification 的生产依赖里没有 :core:data，且生产接收器作用域为 IO——
 * 测试图替换调度绑定为假仓储闭环图、作用域换 Unconfined（goAsync 协程体在 onReceive 内
 * 同步跑完，断言无需等待）。接收器经 EntryPointAccessors 手动取依赖（本仓 Hilt 2.52 + KSP
 * 下 @AndroidEntryPoint 广播接收器不可用，见生产 NotificationModule 内说明）。
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NotificationModule::class])
abstract class TestNotificationModule {
    @Binds
    abstract fun bindReminderScheduler(impl: ReminderSchedulerImpl): ReminderScheduler
}

/** 与生产接收器作用域同形（Supervisor），仅换 Unconfined 调度器保证测试确定性。 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ReceiverScopeModule::class])
object TestReceiverScopeModule {
    @Provides
    @Singleton
    fun provideReceiverScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
}

/** 测试假体绑定：与断言共享 TestFakes 的同一批实例。 */
@Module
@InstallIn(SingletonComponent::class)
object TestFakeGraphModule {
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
