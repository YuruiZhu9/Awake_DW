package com.awakedw.core.model

/** 单日统计。杯数 <2 时 [avgIntervalMin] 为 null（无平均间隔可言）；
 * [lastDrankAtEpochMs] 为当日最后一杯时刻（无记录为 null），供「最近一杯」展示。
 */
data class DailyStats(
    val totalMl: Int,
    val cupCount: Int,
    val avgIntervalMin: Int?,
    val lastDrankAtEpochMs: Long? = null,
)
