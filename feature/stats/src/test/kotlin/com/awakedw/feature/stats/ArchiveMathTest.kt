package com.awakedw.feature.stats

import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.WaterRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 统计档案纯换算（2.1.0）：月历网格几何、热力档位边界、节律分桶与摘要文案。
 * 只陈述事实，不引入成就语义（D10）。
 */
class ArchiveMathTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    /** 2026-08-27 是星期四（周首行偏移 3），8 月共 31 天。 */
    private val todayKey = "2026-08-27"

    @Test
    fun `月历网格周一首行月初空位占位到今天为止`() {
        val cells = ArchiveMath.monthCells(todayKey, totalsByDay = mapOf("2026-08-27" to 750))

        // 周四开头 → 3 个空位，再加 31 天 = 34 格。
        assertEquals(3 + 31, cells.size)
        assertEquals(listOf<MonthCell?>(null, null, null), cells.take(3))
        val day1 = cells[3]!!
        assertEquals(1, day1.dayNumber)
        assertEquals("2026-08-01", day1.dayKey)
        assertEquals(0, day1.totalMl)
        val today = cells.last { it != null && !it.isFuture }!!
        assertEquals(27, today.dayNumber)
        assertTrue(today.isToday)
        assertEquals(750, today.totalMl)
        // 28–31 日尚未到来：占位但不上色。
        val futures = cells.filterNotNull().filter { it.isFuture }
        assertEquals(listOf(28, 29, 30, 31), futures.map { it.dayNumber })
        assertTrue(futures.all { it.totalMl == 0 })
    }

    @Test
    fun `周一是每月一号时网格无前导空位`() {
        // 2026-08-03 是星期一。
        val cells = ArchiveMath.monthCells("2026-08-03", totalsByDay = emptyMap())

        // 无前导空位，首格即 1 日；截至今天（3 日）共 3 个非未来格。
        assertEquals(1, cells.firstOrNull { it != null }?.dayNumber)
        assertEquals(3, cells.filterNotNull().count { !it.isFuture })
    }

    @Test
    fun `日期键解析失败返回空网格不造日期`() {
        assertTrue(ArchiveMath.monthCells("不是日期", emptyMap()).isEmpty())
    }

    @Test
    fun `热力档位按距目标三等分就近落档`() {
        val goal = 1600
        assertEquals(0, ArchiveMath.heatLevel(0, goal))
        assertEquals(1, ArchiveMath.heatLevel(1, goal))
        assertEquals(1, ArchiveMath.heatLevel(533, goal))
        assertEquals(2, ArchiveMath.heatLevel(534, goal))
        assertEquals(2, ArchiveMath.heatLevel(1066, goal))
        assertEquals(3, ArchiveMath.heatLevel(1067, goal))
        assertEquals(3, ArchiveMath.heatLevel(1599, goal))
        assertEquals(4, ArchiveMath.heatLevel(1600, goal))
        assertEquals(4, ArchiveMath.heatLevel(2400, goal))
    }

    @Test
    fun `月历摘要陈述有记录与达标天数`() {
        val totals =
            mapOf(
                "2026-08-01" to 1600,
                "2026-08-02" to 800,
                "2026-08-03" to 0,
                "2026-08-27" to 2000,
            )
        val cells = ArchiveMath.monthCells(todayKey, totals)

        assertEquals("本月有记录 3 天，达标 2 天", ArchiveMath.monthSummary(cells, goalMl = 1600))
    }

    @Test
    fun `月标题转中文月份异常键返回空串`() {
        assertEquals("8月", ArchiveMath.monthTitle(todayKey))
        assertEquals("", ArchiveMath.monthTitle("不是日期"))
    }

    @Test
    fun `节律按早白天晚三段聚合夜间归晚`() {
        fun record(
            id: Long,
            hour: Int,
            minute: Int,
            amountMl: Int,
        ): WaterRecord {
            val at = LocalDateTime.of(2026, 8, 27, hour, minute).atZone(zone).toInstant().toEpochMilli()
            return WaterRecord(id = id, amountMl = amountMl, drankAtEpochMs = at, dayKeyLocal = "2026-08-27")
        }

        val slices =
            ArchiveMath.rhythmOf(
                records =
                    listOf(
                        record(1, hour = 8, minute = 0, amountMl = 250),
                        record(2, hour = 14, minute = 30, amountMl = 300),
                        record(3, hour = 23, minute = 0, amountMl = 200),
                        record(4, hour = 3, minute = 0, amountMl = 100),
                        record(5, hour = 10, minute = 0, amountMl = 150),
                    ),
                zone = zone,
            )

        // 早 6–10：8 点 + 10 点；白天 11–17：14 点半；晚（其余，含夜间）：23 点 + 3 点。
        assertEquals(listOf(400, 300, 300), slices.map { it.totalMl })
        assertEquals(TimeSlot.MORNING, slices[0].slot)
        assertEquals(TimeSlot.DAY, slices[1].slot)
        assertEquals(TimeSlot.EVENING, slices[2].slot)
    }

    @Test
    fun `无记录时节律保留三段全零切片`() {
        val slices = ArchiveMath.rhythmOf(emptyList(), zone)

        assertEquals(listOf(0, 0, 0), slices.map { it.totalMl })
    }

    @Test
    fun `节律标签与摘要使用现代白话`() {
        val slices =
            listOf(
                RhythmSlice(TimeSlot.MORNING, 400),
                RhythmSlice(TimeSlot.DAY, 300),
                RhythmSlice(TimeSlot.EVENING, 0),
            )

        assertEquals("早", ArchiveMath.slotLabel(TimeSlot.MORNING))
        assertEquals("白天", ArchiveMath.slotLabel(TimeSlot.DAY))
        assertEquals("晚", ArchiveMath.slotLabel(TimeSlot.EVENING))
        assertEquals("近七日时段分布：早 400ml，白天 300ml，晚 0ml", ArchiveMath.rhythmSummary(slices))
    }
}
