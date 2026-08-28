package com.awakedw.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.components.BadgeChip
import com.awakedw.core.designsystem.currentThemeSpec

/** 统计双徽章（规格 §3.2 第 3 条）：「今日 {n} 杯 ☀」「平均间隔 {label} ⏱」。 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun BadgesRow(
    cupCount: Int,
    avgIntervalLabel: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BadgeChip(text = "今日 $cupCount 杯 ☀", spec = spec)
        BadgeChip(text = "平均间隔 $avgIntervalLabel ⏱", spec = spec)
    }
}
