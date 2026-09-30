package com.awakedw.feature.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 统计月历纯换算（2.1.0 引入，2.5.0 三档收敛）：网格几何、档位边界、摘要与标题。
 * 只陈述事实，不引入成就语义（D10）。
 */
class ArchiveMathTest {
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
    fun `热力三档只回答喝没喝够没够`() {
        val goal = 1600
        assertEquals(0, ArchiveMath.heatLevel(0, goal))
        assertEquals(1, ArchiveMath.heatLevel(1, goal))
        assertEquals(1, ArchiveMath.heatLevel(1599, goal))
        assertEquals(2, ArchiveMath.heatLevel(1600, goal))
        assertEquals(2, ArchiveMath.heatLevel(2400, goal))
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
}
