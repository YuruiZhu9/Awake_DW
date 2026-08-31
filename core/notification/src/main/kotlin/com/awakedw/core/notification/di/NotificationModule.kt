package com.awakedw.core.notification.di

import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.notification.NotifBuilder
import com.awakedw.core.notification.ReminderScheduler
import com.awakedw.core.notification.ReminderSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    abstract fun bindReminderScheduler(impl: ReminderSchedulerImpl): ReminderScheduler
}

/** 接收器后台工作域：goAsync 窗口内完成落库/重排，失败互不牵连（Supervisor）。 */
@Module
@InstallIn(SingletonComponent::class)
object ReceiverScopeModule {
    @Provides
    @Singleton
    fun provideReceiverScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/**
 * 接收器手动入口：本仓 Hilt 2.52 + KSP 组合下 @AndroidEntryPoint 广播接收器不可用
 * （Kotlin 无法 super 调用抽象 onReceive；显式继承生成基类又令 KSP 报 NonExistentClass），
 * 故接收器在 onReceive 时经 EntryPointAccessors.fromApplication 自取依赖（等价注入时机）。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderGraphEntryPoint {
    fun notifBuilder(): NotifBuilder

    fun copyLibrary(): CopyLibraryRepository

    fun logWater(): LogWaterUseCase

    fun scheduler(): ReminderScheduler

    fun receiverScope(): CoroutineScope
}
