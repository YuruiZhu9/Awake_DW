package com.awakedw.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** 表驱动校验用例：非法输入一律回落原值（不夹紧、不吸附到合法档位）。 */
class SettingsValidationTest {
    // region 目标量 / 一杯容量：∈[200..4000] 且步进 50

    @Test
    fun `目标量合法性——区间内且整除50的值全部通过`() {
        val cases = listOf(200, 250, 1550, 1600, 2500, 3950, 4000)
        cases.forEach { ml -> assertEquals("ml=$ml 应合法", true, SettingsValidation.isValidMl(ml)) }
    }

    @Test
    fun `目标量合法性——越界与步进外的值全部拒绝`() {
        val cases = listOf(Int.MIN_VALUE, -250, 0, 150, 199, 201, 225, 235, 3999, 4001, 4050, Int.MAX_VALUE)
        cases.forEach { ml -> assertEquals("ml=$ml 应拒绝", false, SettingsValidation.isValidMl(ml)) }
    }

    @Test
    fun `一杯容量与目标量共用同一条 200-4000 步进50 规则`() {
        assertEquals(true, SettingsValidation.isValidMl(250))
        assertEquals(false, SettingsValidation.isValidMl(260))
    }

    // endregion

    // region 提醒间隔：仅接受七档候选集

    @Test
    fun `间隔合法性——候选集30到240七档全部通过`() {
        val cases = listOf(30, 60, 90, 120, 150, 180, 240)
        cases.forEach { min -> assertEquals("interval=$min 应合法", true, SettingsValidation.isValidInterval(min)) }
    }

    @Test
    fun `间隔合法性——候选集外的值一律回落（如50不在集内回落90）`() {
        val cases = listOf(0, 15, 45, 50, 75, 200, 210, 300, -90, Int.MAX_VALUE)
        cases.forEach { min -> assertEquals("interval=$min 应拒绝", false, SettingsValidation.isValidInterval(min)) }
    }

    // endregion

    // region 清醒时段：起止 ∈[300..1380]、15min 粒度、start < end-30

    @Test
    fun `时段合法性——默认窗与边界窗全部通过`() {
        val cases =
            listOf(
                // 默认 08:00–22:30
                WindowCase(480, 1350, true),
                // 最早可拖 05:00 起、最晚可拖 23:00 止
                WindowCase(300, 1380, true),
                // 最小合法间隔：相差 45 分钟（15min 粒度下紧贴 30 分钟下限的上一档）
                WindowCase(300, 345, true),
                WindowCase(540, 1290, true),
            )
        cases.forEach { c -> assertEquals("window=${c.start}-${c.end} 应合法", c.expected, SettingsValidation.isValidWindow(c.start, c.end)) }
    }

    @Test
    fun `时段合法性——间隔不足30分钟拒绝（1000与1005只差5分钟）`() {
        assertEquals(false, SettingsValidation.isValidWindow(1000, 1005))
        // 相差恰好 30 分钟也不行：要求严格 start < end-30
        assertEquals(false, SettingsValidation.isValidWindow(480, 510))
    }

    @Test
    fun `时段合法性——越界与非15分钟粒度一律拒绝`() {
        // 依次：起点低于 05:00；起点高于 23:00；终点超过 23:00；
        // 起点不在 15min 粒度上；终点不在 15min 粒度上；倒挂；相等。
        val cases =
            listOf(
                WindowCase(299, 1000, false),
                WindowCase(1381, 1380, false),
                WindowCase(300, 1395, false),
                WindowCase(455, 1000, false),
                WindowCase(300, 1007, false),
                WindowCase(1000, 480, false),
                WindowCase(600, 600, false),
            )
        cases.forEach { c -> assertEquals("window=${c.start}-${c.end} 应拒绝", c.expected, SettingsValidation.isValidWindow(c.start, c.end)) }
    }

    // endregion

    /** 表驱动用例条目：清醒时段（起、止、期望）。 */
    private data class WindowCase(
        val start: Int,
        val end: Int,
        val expected: Boolean,
    )
}
