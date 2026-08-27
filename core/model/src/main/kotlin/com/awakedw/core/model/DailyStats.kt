package com.awakedw.core.model

/** 单日统计。杯数 <2 时 [avgIntervalMin] 为 null（无平均间隔可言）。 */
data class DailyStats(
    val totalMl: Int,
    val cupCount: Int,
    val avgIntervalMin: Int?,
)
