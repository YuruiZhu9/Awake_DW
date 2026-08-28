package com.awakedw.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.awakedw.core.notification.di.ReminderGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors

/** 开机自启：设备重启后按当前设置重排当日提醒链。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            EntryPointAccessors.fromApplication(context.applicationContext, ReminderGraphEntryPoint::class.java)
                .scheduler()
                .rescheduleFromNow(Reason.BOOT)
        }
    }
}
