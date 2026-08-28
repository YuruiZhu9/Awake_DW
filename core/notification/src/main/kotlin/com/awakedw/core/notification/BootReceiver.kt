package com.awakedw.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.awakedw.core.notification.di.ReminderGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * 开机自启：设备重启后按当前设置重排当日提醒链。
 *
 * goAsync 加固：开机冷读（DataStore/Room 首触）在注入的接收器作用域（IO）内完成，
 * 完成后 pendingResult.finish()，规避主线程广播窗口内的磁盘竞争。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val graph =
            EntryPointAccessors.fromApplication(context.applicationContext, ReminderGraphEntryPoint::class.java)
        val scheduler = graph.scheduler()
        graph.receiverScope().launch {
            try {
                scheduler.rescheduleFromNow(Reason.BOOT)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
