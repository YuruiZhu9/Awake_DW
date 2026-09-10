package com.awakedw.core.domain.contracts

import com.awakedw.core.model.TimeSlot
import kotlinx.coroutines.flow.Flow

/**
 * 短句池默认去重窗口：内置短句每时段 10 条，窗口取 4 保证每次至少 6 条候选，
 * 既避免抬手就重复，也不会把池子抽空。
 */
const val SHORT_POOL_AVOID_RECENT = 4

/** 心意文案库仓储：整库可编辑，抽取时避开「最近用过」的句子。（原 :core:data 接口按依赖倒置下沉，成员签名不变） */
interface CopyLibraryRepository {
    val library: Flow<CopyLibrary>

    /**
     * 抽取**心意文案长句**（首页问候语与提醒通知正文）：
     * 从 [slot] 组随机抽一句；跳过最近 [avoidRecent] 条，
     * 候选耗尽则清空该时段去重池重来；组被删空后回退对应默认时段。
     */
    suspend fun randomFor(
        slot: TimeSlot,
        avoidRecent: Int = 5,
    ): String

    /**
     * 抽取**打卡确认短句**：来自内置短句池，与心意文案库的长句互不干扰。
     * 打卡瞬间出现在环心，需要一句 3–14 字的即时回应，而不是完整的心声。
     */
    suspend fun randomPraise(
        slot: TimeSlot,
        avoidRecent: Int = SHORT_POOL_AVOID_RECENT,
    ): String

    /**
     * 抽取**猫咪回应短句**：同样来自内置短句池，与 [randomPraise] 使用两个独立的去重窗口。
     * 旧版 JSON 中的 `cat` 字段只保留为兼容数据，不再作为活动语料池。
     */
    suspend fun randomCatLine(
        slot: TimeSlot,
        avoidRecent: Int = SHORT_POOL_AVOID_RECENT,
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

    /** 整库恢复默认 108 句并清空全部去重池。 */
    suspend fun resetToDefaults()
}
