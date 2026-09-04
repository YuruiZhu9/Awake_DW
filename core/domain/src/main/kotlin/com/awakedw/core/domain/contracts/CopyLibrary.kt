package com.awakedw.core.domain.contracts

import com.awakedw.core.model.TimeSlot
import kotlinx.serialization.Serializable

/** 心意文案库：按早/午/晚时段分组的短句集合，序列化后存于 `copy_library_json`。 */
@Serializable
data class CopyLibrary(
    val morning: List<String>,
    val day: List<String>,
    val evening: List<String>,
    /**
     * 旧版本猫语字段，仅为 JSON 向后兼容保留；当前反馈不再读取它，而是使用对应时段文案组。
     */
    val cat: List<String> = emptyList(),
) {
    /** 取某时段的文案组；无该组数据时返回空列表。 */
    fun groupOf(slot: TimeSlot): List<String> =
        when (slot) {
            TimeSlot.MORNING -> morning
            TimeSlot.DAY -> day
            TimeSlot.EVENING -> evening
        }
}
