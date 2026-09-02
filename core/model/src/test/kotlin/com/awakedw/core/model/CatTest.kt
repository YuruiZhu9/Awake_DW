package com.awakedw.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure mood resolution for the optional visual mascot. */
class CatTest {
    @Test
    fun `celebration takes priority over nighttime`() {
        assertEquals(CatMood.HAPPY, resolveCatMood(justCelebrated = true, nowHour = 23))
    }

    @Test
    fun `night window resolves to sleepy`() {
        assertEquals(CatMood.SLEEPY, resolveCatMood(justCelebrated = false, nowHour = 22))
        assertEquals(CatMood.SLEEPY, resolveCatMood(justCelebrated = false, nowHour = 5))
    }

    @Test
    fun `daytime without celebration resolves to idle`() {
        assertEquals(CatMood.IDLE, resolveCatMood(justCelebrated = false, nowHour = 6))
        assertEquals(CatMood.IDLE, resolveCatMood(justCelebrated = false, nowHour = 21))
    }
}
