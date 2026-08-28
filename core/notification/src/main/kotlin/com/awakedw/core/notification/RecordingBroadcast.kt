package com.awakedw.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.awakedw.core.notification.di.ReminderGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

/**
 * 通知动作「喝啦 💧」落点：同步记一杯（单条 DB 写入，接收器生命周期内完成），
 * 通知原位更新为「记好啦 ♡」+ 2 秒自清，并按 LOGGED 重算下一点（达标即取消剩余）。
 */
class RecordingBroadcast : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val graph =
            EntryPointAccessors.fromApplication(context.applicationContext, ReminderGraphEntryPoint::class.java)
        val logWater = graph.logWater()
        runBlocking { logWater() }
        val notifBuilder = graph.notifBuilder()
        notifBuilder.post(notifBuilder.loggedAck())
        graph.scheduler().rescheduleFromNow(Reason.LOGGED)
    }
}
