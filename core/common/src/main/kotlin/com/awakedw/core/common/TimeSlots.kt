package com.awakedw.core.common

import com.awakedw.core.model.TimeSlot

/** 24 小时制小时 → 时段：6–10→MORNING（含10），11–17→DAY（含17），其余→EVENING。 */
object TimeSlots {
    fun slotOfHour(hour24: Int): TimeSlot =
        when (hour24) {
            in 6..10 -> TimeSlot.MORNING
            in 11..17 -> TimeSlot.DAY
            else -> TimeSlot.EVENING
        }
}
