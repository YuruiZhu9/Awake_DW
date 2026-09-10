package com.awakedw.core.notification

import com.awakedw.core.common.TimeSlots
import com.awakedw.core.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 提醒通知的时段标题映射。
 *
 * 这条映射曾经写错：DAY 覆盖 11:00–17:59（见 [TimeSlots.slotOfHour]），
 * 标题却写成「下午好 ☀」——上午 11 点收到提醒会说错时段。
 * 三档都取不指认具体时辰的说法，避免区间与措辞再次错位。
 */
class NotifTitleTest {
    @Test
    fun `三个时段各有不指认具体时辰的问早语`() {
        assertEquals("早安 ☀", NotifBuilder.titleOf(TimeSlot.MORNING))
        assertEquals("午安 ☀", NotifBuilder.titleOf(TimeSlot.DAY))
        assertEquals("晚上好 🌙", NotifBuilder.titleOf(TimeSlot.EVENING))
    }

    @Test
    fun `DAY横跨中午与下午_标题不得只说下午`() {
        // 11:00 与 17:59 落在同一时段：标题必须同时站得住。
        assertEquals(TimeSlot.DAY, TimeSlots.slotOfHour(11))
        assertEquals(TimeSlot.DAY, TimeSlots.slotOfHour(17))

        val dayTitle = NotifBuilder.titleOf(TimeSlot.DAY)
        assertFalse("11:00 与 17:59 同属 DAY，标题不能只说下午：$dayTitle", dayTitle.contains("下午"))
    }
}
