package com.awakedw.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 2.6.0 动效二阶：交互反馈共享节奏入 token 表后的取值契约。
 * 钉住取值不是为了永远不变，而是让任何调整都必须过一次「有意识改表」的决策。
 */
class MotionTokensTest {
    @Test
    fun `数字滚动共享节奏钉在统一值`() {
        assertEquals(500, MotionTokens.DURATION_NUMBER_ROLL_MS)
    }

    @Test
    fun `打卡三通道进出共享节奏钉在统一值`() {
        assertEquals(240, MotionTokens.DURATION_FEEDBACK_MS)
    }
}
