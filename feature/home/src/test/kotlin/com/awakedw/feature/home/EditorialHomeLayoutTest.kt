package com.awakedw.feature.home

import com.awakedw.feature.home.components.editorialDateLabel
import com.awakedw.feature.home.components.editorialGoalLine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class EditorialHomeLayoutTest {
    @Test
    fun `editorial masthead formats the date as a quiet page heading`() {
        assertEquals("9月24日 · 星期四", editorialDateLabel(LocalDateTime.of(2026, 9, 24, 9, 0)))
    }

    @Test
    fun `editorial goal line stays factual before and after the goal`() {
        assertEquals("每日目标 1600ml · 还差 350ml", editorialGoalLine(totalMl = 1250, goalMl = 1600))
        assertEquals("每日目标 1600ml · 已达成", editorialGoalLine(totalMl = 1600, goalMl = 1600))
    }
}
