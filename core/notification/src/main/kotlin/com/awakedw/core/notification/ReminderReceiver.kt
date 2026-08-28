package com.awakedw.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.awakedw.core.notification.di.ReminderGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * 闹钟到点：按当前时段抽一句文案发「温柔提醒」，随后链式排下一点
 * （若当日已达标，重算返回 null 即短路取消）。
 *
 * goAsync 加固：落库前读设置、文案抽取与重排均在注入的接收器作用域（IO）内完成，
 * 完成后 pendingResult.finish()，不阻塞主线程广播窗口。
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        val graph =
            EntryPointAccessors.fromApplication(context.applicationContext, ReminderGraphEntryPoint::class.java)
        val notifBuilder = graph.notifBuilder()
        val copies = graph.copyLibrary()
        val scheduler = graph.scheduler()
        graph.receiverScope().launch {
            try {
                val slot = notifBuilder.currentSlot()
                val body = copies.randomFor(slot)
                notifBuilder.post(notifBuilder.reminder(slot, body))
                scheduler.rescheduleFromNow(Reason.APP_START)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
