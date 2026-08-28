package com.awakedw.app

import android.app.Application
import com.awakedw.core.notification.Reason
import com.awakedw.core.notification.di.ReminderGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用宿主：@HiltAndroidApp 生成 SingletonComponent，为全图闭环的装配入口。
 *
 * APP_START 接线（集成决议，恰此一处）：进程冷启动时经 EntryPoint 取调度器
 * `rescheduleFromNow(APP_START)` 重排当日下一点——关闭提醒/达标/越过窗口时
 * 重算短路为取消，天然幂等；每进程恰一次，不与设置开关/接收器重排重复触发。
 */
@HiltAndroidApp
class AwakeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EntryPointAccessors.fromApplication(this, ReminderGraphEntryPoint::class.java)
            .scheduler()
            .rescheduleFromNow(Reason.APP_START)
    }
}
