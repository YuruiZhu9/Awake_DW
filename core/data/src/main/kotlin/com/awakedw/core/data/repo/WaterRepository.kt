package com.awakedw.core.data.repo

import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar

/** 饮水记录仓储：写入杯量并按本地日聚合。 */
interface WaterRepository {
    suspend fun addCup(amountMl: Int): WaterRecord

    suspend fun todayStats(): DailyStats

    /** 含今天，共 [daysBack] 天，缺数天补 0。 */
    suspend fun weekBars(daysBack: Int = 7): List<WeekBar>

    /** 今日记录，时间升序。 */
    suspend fun todayRecords(): List<WaterRecord>
}
