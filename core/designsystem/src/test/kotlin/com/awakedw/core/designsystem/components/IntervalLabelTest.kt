package com.awakedw.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** 平均间隔文案三档：破折号 / 分钟 / 小时（首页与统计页共用实现，VM 测试另有链路级断言）。 */
class IntervalLabelTest {
    @Test
    fun `无平均间隔显示破折号`() {
        assertEquals("—", IntervalLabel.format(null))
    }

    @Test
    fun `不足90分钟用分钟文案`() {
        assertEquals("45 分钟", IntervalLabel.format(45))
    }

    @Test
    fun `不小于90分钟折叠为小时`() {
        assertEquals("1.6h", IntervalLabel.format(96))
    }
}
