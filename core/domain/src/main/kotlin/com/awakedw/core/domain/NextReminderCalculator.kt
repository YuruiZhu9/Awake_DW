package com.awakedw.core.domain

import com.awakedw.core.common.AppClock
import com.awakedw.core.model.UserSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 分钟数（当日 0 点起算）→ 当日该时刻的 epoch 毫秒。 */
private fun minuteToEpochMs(
    day: LocalDate,
    minuteOfDay: Int,
    zone: ZoneId,
): Long {
    val at = ZonedDateTime.of(day, LocalTime.of(minuteOfDay / 60, minuteOfDay % 60), zone)
    return at.toInstant().toEpochMilli()
}

/**
 * 下一杯提醒时刻计算（无状态纯函数；null 表示今日不再排程）：
 * 1. 提醒关闭或今日已达标 → null；
 * 2. 当前早于窗口起点 → 今天起点整点火；
 * 3. 窗口内 → max(now, 今日起点) + intervalMin；
 * 4. 结果越过当天窗口终点 → null（恰压终点仍算有效排程）。
 */
object NextReminderCalculator {
    fun nextFire(
        s: UserSettings,
        clock: AppClock,
        achievedToday: Boolean,
    ): Long? {
        if (!s.remindersEnabled || achievedToday) {
            return null
        }
        val now = clock.nowEpochMs()
        val zone = clock.zone()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val startMs = minuteToEpochMs(today, s.windowStartMin, zone)
        if (now < startMs) {
            return startMs
        }
        val fireAt = maxOf(now, startMs) + s.intervalMin * MS_PER_MINUTE
        val endMs = minuteToEpochMs(today, s.windowEndMin, zone)
        return if (fireAt <= endMs) fireAt else null
    }

    private const val MS_PER_MINUTE = 60_000L
}
