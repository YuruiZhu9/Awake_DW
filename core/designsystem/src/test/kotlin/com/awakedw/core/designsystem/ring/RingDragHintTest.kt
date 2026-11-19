package com.awakedw.core.designsystem.ring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 空态拖环提示的运动学（2.10.0）：提示珠只做一次正弦往返，不改变真实进度。
 * 这里钉住「起止静止、峰值 26°、两侧对称、越界夹紧」，避免提示变成循环动画或越拖越远。
 */
class RingDragHintTest {
    @Test
    fun `提示珠从静止出发峰值二十六度再回到静止`() {
        assertEquals(0f, dragHintSweepAt(0f), 0.001f)
        assertEquals(26f, dragHintSweepAt(0.5f), 0.001f)
        assertEquals(0f, dragHintSweepAt(1f), 0.001f)
        assertTrue("前四分之一应已经开始移动", dragHintSweepAt(0.25f) > 10f)
        assertTrue("后四分之一应与前半对称", dragHintSweepAt(0.75f) > 10f)
        assertEquals(dragHintSweepAt(0.25f), dragHintSweepAt(0.75f), 0.001f)
    }

    @Test
    fun `提示进度越界时夹在零到一之间`() {
        assertEquals(0f, dragHintSweepAt(-0.5f), 0.001f)
        assertEquals(0f, dragHintSweepAt(1.5f), 0.001f)
    }
}
