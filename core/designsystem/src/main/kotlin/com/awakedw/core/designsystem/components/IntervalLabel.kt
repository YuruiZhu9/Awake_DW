package com.awakedw.core.designsystem.components

import java.util.Locale

/**
 * 平均间隔徽章文案（首页/统计页共用的唯一实现）：
 * 无平均间隔可言（null，即杯数 <2）→「—」；不足 90 分钟 →「X 分钟」；
 * 否则折叠为小时（如 1.6h，固定 US locale 保证小数点）。
 */
object IntervalLabel {
    /** 低于该分钟数用「X 分钟」文案，否则折叠为小时。 */
    private const val HOURLY_LABEL_MIN = 90

    /** 无平均间隔可言时的占位破折号。 */
    private const val DASH = "—"

    fun format(avgIntervalMin: Int?): String =
        when {
            avgIntervalMin == null -> DASH
            avgIntervalMin < HOURLY_LABEL_MIN -> "$avgIntervalMin 分钟"
            else -> String.format(Locale.US, "%.1fh", avgIntervalMin / 60.0)
        }
}
