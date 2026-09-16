package com.awakedw.core.designsystem.gl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AGSL 雾光层门控（alpha13 §13.4）：版本 + 减少动态双条件，任一不满足即静默回退。 */
class AgslGateTest {
    @Test
    fun `雾光仅在Android13及以上且非减少动态时启用`() {
        assertFalse("API 32 无 RuntimeShader", agslSupported(sdkInt = 32, reduceMotion = false))
        assertTrue(agslSupported(sdkInt = 33, reduceMotion = false))
        assertTrue(agslSupported(sdkInt = 35, reduceMotion = false))
    }

    @Test
    fun `减少动态模式下任何版本都不启用`() {
        assertFalse(agslSupported(sdkInt = 33, reduceMotion = true))
        assertFalse(agslSupported(sdkInt = 35, reduceMotion = true))
    }
}
