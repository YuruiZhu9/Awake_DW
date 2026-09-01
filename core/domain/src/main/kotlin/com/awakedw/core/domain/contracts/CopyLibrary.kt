package com.awakedw.core.domain.contracts

import com.awakedw.core.model.TimeSlot
import kotlinx.serialization.Serializable

/** 关心文案库：按时段分组的短句集合，另附胆大王猫语组，序列化后存于 `copy_library_json`。 */
@Serializable
data class CopyLibrary(
    val morning: List<String>,
    val day: List<String>,
    val evening: List<String>,
    /** 胆大王（猫）语料组。v0.2 追加：旧版 JSON 无此字段，kotlinx 对缺失字段取默认值回落空列表（序列化向后兼容）。 */
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
