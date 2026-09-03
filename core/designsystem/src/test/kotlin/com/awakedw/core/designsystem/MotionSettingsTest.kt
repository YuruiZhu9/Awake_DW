package com.awakedw.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/** The reduced-motion policy must remove decorative timing without changing content semantics. */
class MotionSettingsTest {
    @Test
    fun `reduced motion collapses animation duration`() {
        assertEquals(0, motionDurationMillis(durationMillis = 200, reduceMotion = true))
    }

    @Test
    fun `normal motion keeps the designed duration`() {
        assertEquals(500, motionDurationMillis(durationMillis = 500, reduceMotion = false))
    }
}
