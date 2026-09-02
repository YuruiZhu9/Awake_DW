package com.awakedw.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserSettingsTest {
    @Test
    fun `默认值为设计与文档常量`() {
        val s = UserSettings()
        assertEquals(1600, s.goalMl)
        assertEquals(250, s.cupMl)
        assertEquals(480, s.windowStartMin)
        assertEquals(1350, s.windowEndMin)
        assertEquals(90, s.intervalMin)
        assertEquals(true, s.remindersEnabled)
        assertEquals(ThemeChoice.FOLLOW_TIME, s.themeChoice)
    }
}
