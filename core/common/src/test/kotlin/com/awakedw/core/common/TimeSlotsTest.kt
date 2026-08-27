package com.awakedw.core.common

import com.awakedw.core.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeSlotsTest {
    @Test fun `时段边界`() {
        assertEquals(TimeSlot.EVENING, TimeSlots.slotOfHour(5))
        assertEquals(TimeSlot.MORNING, TimeSlots.slotOfHour(6))
        assertEquals(TimeSlot.MORNING, TimeSlots.slotOfHour(10))
        assertEquals(TimeSlot.DAY, TimeSlots.slotOfHour(11))
        assertEquals(TimeSlot.DAY, TimeSlots.slotOfHour(17))
        assertEquals(TimeSlot.EVENING, TimeSlots.slotOfHour(18))
        assertEquals(TimeSlot.EVENING, TimeSlots.slotOfHour(3))
    }

    @Test fun `全小时映射无遗漏`() {
        for (hour in 0..23) {
            val expected =
                when (hour) {
                    in 6..10 -> TimeSlot.MORNING
                    in 11..17 -> TimeSlot.DAY
                    else -> TimeSlot.EVENING
                }
            assertEquals("hour=$hour", expected, TimeSlots.slotOfHour(hour))
        }
    }

    @Test fun `dayKey 按本地时区切日`() {
        // 2026-08-27 22:00 UTC+8 == 同日本地日
        val ms = ZonedDateTime.of(2026, 8, 27, 22, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        assertEquals("2026-08-27", ms.toDayKey(ZoneId.of("Asia/Shanghai")))
    }

    @Test fun `dayKey 跨时区翻转`() {
        // 2026-08-27 01:00 UTC+8 == 2026-08-26 13:00 美东（EDT, UTC-4），同一时刻本地日期不同
        val ms = ZonedDateTime.of(2026, 8, 27, 1, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        assertEquals("2026-08-26", ms.toDayKey(ZoneId.of("America/New_York")))
    }

    @Test fun `SystemAppClock 提供当前时间与系统时区`() {
        val before = System.currentTimeMillis()
        val clock = SystemAppClock()
        val now = clock.nowEpochMs()
        val after = System.currentTimeMillis()
        assert(now in before..after)
        assertEquals(ZoneId.systemDefault(), clock.zone())
    }
}
