package com.awakedw.core.domain

import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.WaterRecord
import javax.inject.Inject

/**
 * 删除一笔饮水记录（纠错路径）。
 *
 * 记录是使用者自己按下的，就应当可以自己收回：首页「最近一杯」长按撤回刚记的那杯，
 * 统计页时间线长按删掉任意一笔。删除后由仓储的变更流驱动首页与统计页重算，本用例不持有状态。
 *
 * 不触碰 `celebrated_day_key`：撤回后再达标不会重复庆祝，
 * 避免「撤回—记录」来回操作反复刷出庆祝反馈。
 */
class DeleteWaterRecordUseCase
    @Inject
    constructor(
        private val water: WaterRepository,
    ) {
        /** 按 id 删除指定一笔（统计页时间线）；id 不存在时静默忽略。 */
        suspend operator fun invoke(recordId: Long) {
            water.delete(recordId)
        }

        /**
         * 撤回今日最后一笔（首页「最近一杯」长按）。
         *
         * 返回被删掉的那一笔，供调用方给出「删掉了哪一杯」的确认；
         * 今日没有记录时返回 null，不做任何写入。
         */
        suspend fun revertLatestToday(): WaterRecord? {
            val latest = water.todayRecords().lastOrNull() ?: return null
            water.delete(latest.id)
            return latest
        }
    }
