package com.awakedw.core.model

/** 一次喝水记录。[dayKeyLocal] 为记录时刻本地日期键（yyyy-MM-dd），用于按天聚合。 */
data class WaterRecord(
    val id: Long = 0L,
    val amountMl: Int,
    val drankAtEpochMs: Long,
    val dayKeyLocal: String,
)
