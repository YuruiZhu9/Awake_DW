package com.awakedw.core.domain.contracts

import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.flow.Flow

/** 饮水记录仓储：写入杯量并按本地日聚合。（原 :core:data 接口按依赖倒置下沉，成员签名不变） */
interface WaterRepository {
    /** 水库变更触发器：任何写入都发射一次 Unit（Room 行数流，首值即当前态）。 */
    val changes: Flow<Unit>

    suspend fun addCup(amountMl: Int): WaterRecord

    suspend fun todayStats(): DailyStats

    /** 含今天，共 [daysBack] 天，缺数天补 0。 */
    suspend fun weekBars(daysBack: Int = 7): List<WeekBar>

    /** 今日记录，时间升序。 */
    suspend fun todayRecords(): List<WaterRecord>
}
