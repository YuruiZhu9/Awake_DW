package com.awakedw.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 环即把手的纯换算：整圈一杯、就近取整 10 的倍数、阈值内与不足 10ml 都取消（v220 计划 §测试）。 */
class RingDragMathTest {
    @Test
    fun `整圈恰好一杯容量`() {
        assertEquals(250, RingDragMath.sweepToMl(360f, cupMl = 250))
    }

    @Test
    fun `半圈与四分之一圈就近取整到10的倍数`() {
        assertEquals(60, RingDragMath.sweepToMl(90f, cupMl = 250))
        assertEquals(130, RingDragMath.sweepToMl(180f, cupMl = 250))
    }

    @Test
    fun `提交阈值处给出最小可记量`() {
        assertEquals(10, RingDragMath.sweepToMl(15f, cupMl = 250))
    }

    @Test
    fun `阈值内与倒拨都不成笔`() {
        assertNull(RingDragMath.sweepToMl(14.9f, cupMl = 250))
        assertNull(RingDragMath.sweepToMl(-90f, cupMl = 250))
        assertNull(RingDragMath.sweepToMl(0f, cupMl = 250))
    }

    @Test
    fun `换算不足10ml视为取消杜绝0ml记录`() {
        assertNull(RingDragMath.sweepToMl(15f, cupMl = 50))
        assertNull(RingDragMath.sweepToMl(15f, cupMl = 30))
    }
}
