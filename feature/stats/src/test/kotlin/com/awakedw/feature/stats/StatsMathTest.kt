package com.awakedw.feature.stats

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 统计页柱状图的纯几何换算（规格 §3.3 第 2 条）：
 * 柱高与目标线共用同一刻度，最大柱至多图表高 × 0.86，0 值柱交给绘制层画基线圆点。
 */
class StatsMathTest {
    /** 测试用图表高 100px，0.86 刻度带即 86px，换算心算可验。 */
    private val chartHeight = 100f

    @Test
    fun `全零周所有柱都归一到基线圆点`() {
        val heights = StatsMath.barHeights(values = List(7) { 0 }, goalMl = 1600, chartHeight = chartHeight)

        assertEquals(List(7) { 0f }, heights)
    }

    @Test
    fun `单柱独大时最大柱顶到0点86倍图高其余为基线`() {
        val values = listOf(0, 0, 0, 0, 0, 0, 2000)

        val heights = StatsMath.barHeights(values = values, goalMl = 1600, chartHeight = chartHeight)

        assertEquals(listOf(0f, 0f, 0f, 0f, 0f, 0f, 86f), heights.map { it })
    }

    @Test
    fun `所有柱都低于目标时目标线落在刻度带顶端`() {
        val values = listOf(200, 400, 800, 300, 600, 500, 750)

        val y = StatsMath.goalLineY(goalMl = 1600, values = values, chartHeight = chartHeight)

        assertEquals(100f - 86f, y, EPSILON)
    }

    @Test
    fun `最大柱越过目标时目标线按同一刻度落在柱身之间`() {
        // 刻度顶 = 2000，目标 1600 → 线距顶 1600/2000 × 86 = 68.8，y = 31.2。
        val values = listOf(500, 1200, 2000, 900, 700, 1100, 1400)

        val y = StatsMath.goalLineY(goalMl = 1600, values = values, chartHeight = chartHeight)

        assertEquals(100f - 1600f / 2000f * 86f, y, EPSILON)
        // 同时最大柱仍恰好顶到 0.86 倍图高。
        val heights = StatsMath.barHeights(values = values, goalMl = 1600, chartHeight = chartHeight)
        assertEquals(86f, heights[2], EPSILON)
    }

    @Test
    fun `周内无任何柱时目标线仍以目标量为刻度顶落位`() {
        val y = StatsMath.goalLineY(goalMl = 1600, values = List(7) { 0 }, chartHeight = chartHeight)

        assertEquals(100f - 86f, y, EPSILON)
    }

    @Test
    fun `达标柱按目标量逐日标记主色其余走轨道色`() {
        val flags = StatsMath.metGoal(values = listOf(1600, 800, 2000, 0), goalMl = 1600)

        assertEquals(listOf(true, false, true, false), flags)
    }

    @Test
    fun `末列今天标注今字其余列标注当月几号`() {
        val dayKeys = List(7) { "2026-08-${21 + it}" }

        val labels = StatsMath.columnLabels(dayKeys = dayKeys, todayKey = "2026-08-27")

        assertEquals(listOf("21", "22", "23", "24", "25", "26", "今"), labels)
    }

    private companion object {
        const val EPSILON = 1e-3f
    }
}
