package com.awakedw.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.awakedw.core.notification.di.ReminderGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

/**
 * 闹钟到点：按当前时段抽一句文案发「温柔提醒」，随后链式排下一点
 * （若当日已达标，重算返回 null 即短路取消）。
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val graph =
            EntryPointAccessors.fromApplication(context.applicationContext, ReminderGraphEntryPoint::class.java)
        val notifBuilder = graph.notifBuilder()
        val slot = notifBuilder.currentSlot()
        val body = runBlocking { graph.copyLibrary().randomFor(slot) }
        notifBuilder.post(notifBuilder.reminder(slot, body))
        graph.scheduler().rescheduleFromNow(Reason.APP_START)
    }
}
