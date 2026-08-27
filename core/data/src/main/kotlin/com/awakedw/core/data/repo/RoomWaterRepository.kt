package com.awakedw.core.data.repo

import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.data.db.WaterRecordDao
import com.awakedw.core.data.db.WaterRecordEntity
import com.awakedw.core.data.db.toDomain
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt

/** SQL 聚合 + 本地日历回推的 Room 实现（不感知业务时段，只认 dayKey）。 */
class RoomWaterRepository
    @Inject
    constructor(
        private val dao: WaterRecordDao,
        private val clock: AppClock,
    ) : WaterRepository {
        /** 行数流映射为 Unit：任何写入都会触发一次发射。 */
        override val changes: Flow<Unit> = dao.observeRowCount().map { }

        override suspend fun addCup(amountMl: Int): WaterRecord {
            val now = clock.nowEpochMs()
            val entity = WaterRecordEntity(amountMl = amountMl, drankAtEpochMs = now, dayKeyLocal = now.toDayKey(clock.zone()))
            return entity.copy(id = dao.insert(entity)).toDomain()
        }

        override suspend fun todayStats(): DailyStats {
            val day = currentDayKey()
            val records = dao.recordsFor(day)
            return DailyStats(totalMl = dao.sumFor(day), cupCount = records.size, avgIntervalMin = avgIntervalMinOf(records))
        }

        override suspend fun weekBars(daysBack: Int): List<WeekBar> {
            require(daysBack > 0) { "daysBack 必须为正数" }
            // 以本地日历自今天回推 daysBack 个日键（DST 安全）；缺数天由 sum 查不到而补 0。
            val today = Instant.ofEpochMilli(clock.nowEpochMs()).atZone(clock.zone()).toLocalDate()
            val dayKeys = ((daysBack - 1) downTo 0).map { offset -> today.minusDays(offset.toLong()).toDayKey() }
            val sumsByDay = dao.sumsBetween(from = dayKeys.first(), to = dayKeys.last()).associate { it.d to it.s }
            return dayKeys.map { key -> WeekBar(dayKey = key, totalMl = sumsByDay[key] ?: 0) }
        }

        override suspend fun todayRecords(): List<WaterRecord> = dao.recordsFor(currentDayKey()).map { it.toDomain() }

        private fun currentDayKey(): String = clock.nowEpochMs().toDayKey(clock.zone())

        /** 杯数 <2 时无平均间隔可言；否则取首尾时间跨度 /(n-1)，换算为分钟并四舍五入。 */
        private fun avgIntervalMinOf(records: List<WaterRecordEntity>): Int? {
            if (records.size < 2) return null
            val spanMs = (records.last().drankAtEpochMs - records.first().drankAtEpochMs).toDouble()
            return (spanMs / (records.size - 1) / 60_000.0).roundToInt()
        }
    }
