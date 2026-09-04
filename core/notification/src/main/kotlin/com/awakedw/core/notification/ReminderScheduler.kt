package com.awakedw.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.awakedw.core.common.AppClock
import com.awakedw.core.domain.NextReminderCalculator
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** 重排动机：当前四种动机的重算语义一致，枚举按规格冻结保留，供未来差异化策略。 */
enum class Reason {
    APP_START,
    SETTINGS_CHANGED,
    BOOT,
    LOGGED,
}

/** 喝水提醒调度器：以「现在」为基准重算下一点并写 AlarmManager（null = 今日不再排程，取消收尾）。 */
interface ReminderScheduler {
    fun rescheduleFromNow(reason: Reason)

    fun cancelAll()
}

/**
 * 精确闹钟实现：
 * - 每次重排先取消旧点再定新点，短路路径（关闭 / 达标 / 越过窗口）天然收口为「队列干净」；
 * - SDK ≥ 31 且 `canScheduleExactAlarms()==false` 时降级 [AlarmManager.setWindow]（10 分钟窗口），
 *   否则 [AlarmManager.setExactAndAllowWhileIdle]；
 * - PendingIntent 常驻：requestCode 1001、FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE。
 */
@Singleton
class ReminderSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val prefs: UserPreferencesRepository,
        private val water: WaterRepository,
        private val clock: AppClock,
    ) : ReminderScheduler {
        override fun rescheduleFromNow(reason: Reason) {
            val settings = runBlocking { prefs.settings.first() }
            val achievedToday = runBlocking { water.todayStats().totalMl >= settings.goalMl }

            cancelAll()
            val fireAt = NextReminderCalculator.nextFire(settings, clock, achievedToday) ?: return

            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pi = pendingIntent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, fireAt, EXACT_FALLBACK_WINDOW_MS, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
            }
        }

        override fun cancelAll() {
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent())
        }

        private fun pendingIntent(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE_REMINDER),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        companion object {
            const val REQUEST_CODE = 1001
            const val ACTION_FIRE_REMINDER = "com.awakedw.core.notification.action.FIRE_REMINDER"

            /** 精确闹钟不可用时的降级窗口宽度（§4.3：10 分钟）。 */
            const val EXACT_FALLBACK_WINDOW_MS = 10 * 60_000L
        }
    }
