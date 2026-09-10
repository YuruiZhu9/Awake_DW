package com.awakedw.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.awakedw.core.domain.LogResult
import com.awakedw.core.notification.di.ReminderGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * 通知动作「记一杯」落点：同步记一杯（设置读 + 单条 DB 写 + 当日统计），通知原位更新为
 * 「已记一杯 + 当日进度」+ 2 秒自清，并按 LOGGED 重算下一点（达标即取消剩余）。
 *
 * goAsync 加固：LogWaterUseCase 与重排在注入的接收器作用域（IO）内完成，
 * 完成后 pendingResult.finish()，不阻塞主线程广播窗口。
 */
class RecordingBroadcast : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        val graph =
            EntryPointAccessors.fromApplication(context.applicationContext, ReminderGraphEntryPoint::class.java)
        val logWater = graph.logWater()
        val notifBuilder = graph.notifBuilder()
        val scheduler = graph.scheduler()
        graph.receiverScope().launch {
            try {
                // when 穷举 LogResult：将来新增结果类型时这里会编译不过，逼着补齐回执语义，
                // 而不是让接收器在运行时抛 CastException。
                val (totalMl, celebrated) =
                    when (val result = logWater()) {
                        is LogResult.Logged -> result.totalAfterLog to result.celebrated
                    }
                notifBuilder.post(notifBuilder.loggedAck(totalMl = totalMl, celebrated = celebrated))
                scheduler.rescheduleFromNow(Reason.LOGGED)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
