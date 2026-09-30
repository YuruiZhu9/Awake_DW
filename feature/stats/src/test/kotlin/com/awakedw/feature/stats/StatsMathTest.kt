package com.awakedw.feature.stats

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 统计页「记录」形态的纯换算（2.5.0 去图表化后）：周合计、达标天数、
 * 日期读法、周记录行标签与细轨语义。图表换算已随柱状图撤除。
 */
class StatsMathTest {
    @Test
    fun `近七日合计满一升降单位不足一升保留毫升`() {
        assertEquals("1.9 L", StatsMath.weekTotalLabel(1900))
        assertEquals("1 L", StatsMath.weekTotalLabel(1000))
        assertEquals("820 ml", StatsMath.weekTotalLabel(820))
        assertEquals("0 ml", StatsMath.weekTotalLabel(0))
    }

    @Test
    fun `近七日达标天数按目标逐日判定`() {
        val values = listOf(1600, 800, 2000, 0, 1599, 1601, 1600)
        assertEquals("4/7 天", StatsMath.weekMetDaysLabel(values, goalMl = 1600))
        assertEquals("0/7 天", StatsMath.weekMetDaysLabel(listOf(0, 0, 0, 0, 0, 0, 0), goalMl = 1600))
    }

    @Test
    fun `日期读数转中文月日异常键原样返回`() {
        assertEquals("9月8日", StatsMath.dayReadout("2026-09-08"))
        assertEquals("12月31日", StatsMath.dayReadout("2026-12-31"))
        // 解析失败不造日期：原样返回原始键。
        assertEquals("不是日期", StatsMath.dayReadout("不是日期"))
    }

    @Test
    fun `周记录行标签今天昨天其余转中文月日`() {
        assertEquals("今天", StatsMath.weekRowLabel("2026-09-08", todayKey = "2026-09-08"))
        assertEquals("昨天", StatsMath.weekRowLabel("2026-09-07", todayKey = "2026-09-08"))
        assertEquals("9月3日", StatsMath.weekRowLabel("2026-09-03", todayKey = "2026-09-08"))
        // 键解析失败不造称呼：原样返回。
        assertEquals("不是日期", StatsMath.weekRowLabel("不是日期", todayKey = "2026-09-08"))
    }

    @Test
    fun `细轨进度语义达标即陈述未达标给整除百分比`() {
        assertEquals("今日已达标", StatsMath.todayProgressLabel(1600, goalMl = 1600))
        assertEquals("今日已达标", StatsMath.todayProgressLabel(2000, goalMl = 1600))
        // 1250/1600 = 78.125 → 整除 78，不满不虚报。
        assertEquals("今日进度 78%", StatsMath.todayProgressLabel(1250, goalMl = 1600))
        assertEquals("今日进度 0%", StatsMath.todayProgressLabel(0, goalMl = 1600))
    }
}
