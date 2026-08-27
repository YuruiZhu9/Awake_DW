package com.awakedw.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** 区间按天聚合的中间结果：[d] 为 day_key_local，[s] 为该日 amount_ml 之和。 */
data class DaySum(val d: String, val s: Int)

// "yyyy-MM-dd" 字典序即时间序，BETWEEN 可直接用于本地日区间查询。
private const val SUMS_BETWEEN_SQL =
    "SELECT day_key_local AS d, COALESCE(SUM(amount_ml),0) AS s " +
        "FROM water_record WHERE day_key_local BETWEEN :from AND :to GROUP BY day_key_local"

@Dao
interface WaterRecordDao {
    @Insert
    suspend fun insert(entity: WaterRecordEntity): Long

    @Query("SELECT COALESCE(SUM(amount_ml),0) FROM water_record WHERE day_key_local = :day")
    suspend fun sumFor(day: String): Int

    @Query("SELECT * FROM water_record WHERE day_key_local = :day ORDER BY drank_at_epoch_ms ASC")
    suspend fun recordsFor(day: String): List<WaterRecordEntity>

    @Query(SUMS_BETWEEN_SQL)
    suspend fun sumsBetween(
        from: String,
        to: String,
    ): List<DaySum>
}
