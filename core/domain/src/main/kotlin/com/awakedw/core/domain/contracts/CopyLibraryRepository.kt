package com.awakedw.core.domain.contracts

import com.awakedw.core.model.TimeSlot
import kotlinx.coroutines.flow.Flow

/** 关心文案库仓储：整库可编辑，抽取时避开「最近用过」的句子。（原 :core:data 接口按依赖倒置下沉，成员签名不变） */
interface CopyLibraryRepository {
    val library: Flow<CopyLibrary>

    /**
     * 从 [slot] 组随机抽一句；跳过最近 [avoidRecent] 条，
     * 候选耗尽则清空该时段去重池重来；组被删空后回退任一默认组句子。
     */
    suspend fun randomFor(
        slot: TimeSlot,
        avoidRecent: Int = 5,
    ): String

    /** [index] 在组内则替换；等于或超出组长度时按追加处理（宽容语义）。 */
    suspend fun upsert(
        slot: TimeSlot,
        index: Int,
        text: String,
    )

    /** 删除组内第 [index] 条；越界时静默忽略。 */
    suspend fun delete(
        slot: TimeSlot,
        index: Int,
    )

    /** 整库恢复默认 30 句并清空去重池。 */
    suspend fun resetToDefaults()
}
