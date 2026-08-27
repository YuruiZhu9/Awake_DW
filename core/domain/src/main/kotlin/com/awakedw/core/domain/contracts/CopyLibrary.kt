package com.awakedw.core.domain.contracts

import com.awakedw.core.model.TimeSlot
import kotlinx.serialization.Serializable

/** 关心文案库：按时段分组的短句集合，序列化后存于 `copy_library_json`。 */
@Serializable
data class CopyLibrary(
    val morning: List<String>,
    val day: List<String>,
    val evening: List<String>,
) {
    /** 取某时段的文案组；无该组数据时返回空列表。 */
    fun groupOf(slot: TimeSlot): List<String> =
        when (slot) {
            TimeSlot.MORNING -> morning
            TimeSlot.DAY -> day
            TimeSlot.EVENING -> evening
        }
}
