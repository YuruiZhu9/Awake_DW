package com.awakedw.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.components.BadgeChip
import com.awakedw.core.designsystem.currentThemeSpec

/** Current factual summaries: cups, latest time, and average interval. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun BadgesRow(
    cupCount: Int,
    avgIntervalLabel: String,
    lastDrinkLabel: String?,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BadgeChip(text = "今日 $cupCount 杯 ☀", spec = spec)
        if (lastDrinkLabel != null) {
            BadgeChip(text = "最近一杯 $lastDrinkLabel", spec = spec)
        }
        BadgeChip(text = "平均间隔 $avgIntervalLabel ⏱", spec = spec)
    }
}
