package com.awakedw.core.designsystem.scene

import com.awakedw.core.common.TimeSlots
import com.awakedw.core.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 场景氛围锚点（alpha13 §13）：按时段的装饰强度分段，取值受值域守护。 */
class SceneSpecTest {
    @Test
    fun `夜晚中景最盛_清晨次之_正午最收敛`() {
        val morning = sceneSpecOf(TimeSlot.MORNING)
        val day = sceneSpecOf(TimeSlot.DAY)
        val evening = sceneSpecOf(TimeSlot.EVENING)
        assertTrue(evening.midgroundAlpha > morning.midgroundAlpha)
        assertTrue(morning.midgroundAlpha > day.midgroundAlpha)
    }

    @Test
    fun `清晨粒子最亮_夜晚收暗让位中景`() {
        val morning = sceneSpecOf(TimeSlot.MORNING)
        val day = sceneSpecOf(TimeSlot.DAY)
        val evening = sceneSpecOf(TimeSlot.EVENING)
        assertTrue(morning.particleBoost > day.particleBoost)
        assertTrue(day.particleBoost > evening.particleBoost)
    }

    @Test
    fun `昼间场景即中性默认值`() {
        assertEquals(SceneSpec.DAY, sceneSpecOf(TimeSlot.DAY))
    }

    @Test
    fun `时段划分与业务TimeSlots一致`() {
        // 边界抽样：早晨 < 11、白天 11–18、夜晚 >= 19（与提醒通知同一套划分）。
        assertEquals(TimeSlot.MORNING, TimeSlots.slotOfHour(7))
        assertEquals(TimeSlot.DAY, TimeSlots.slotOfHour(14))
        assertEquals(TimeSlot.EVENING, TimeSlots.slotOfHour(22))
        listOf(TimeSlot.MORNING, TimeSlot.DAY, TimeSlot.EVENING).forEach { slot ->
            val spec = sceneSpecOf(slot)
            assertTrue(spec.midgroundAlpha in 0f..1f)
            assertTrue(spec.particleBoost in 0.5f..1.5f)
        }
    }
}
