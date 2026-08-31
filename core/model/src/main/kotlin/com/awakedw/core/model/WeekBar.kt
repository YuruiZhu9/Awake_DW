package com.awakedw.core.model

/** 首页周条目：某天（[dayKey]，yyyy-MM-dd）的饮水总量（毫升）。 */
data class WeekBar(
    val dayKey: String,
    val totalMl: Int,
)
