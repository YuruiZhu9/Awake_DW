package com.awakedw.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.components.BadgeChip
import com.awakedw.core.designsystem.currentThemeSpec

/**
 * 首页徽章行（§11.2 扩容）：今日杯数 / 最近一杯时刻（有记录时）/ 平均间隔 / 连续达标（≥2 天）。
 * FlowRow 自动换行——窄屏四枚不挤迫。
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun BadgesRow(
    cupCount: Int,
    avgIntervalLabel: String,
    lastDrinkLabel: String?,
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    // 布局审计 P2-2：换行时行间距 8dp——窄屏四枚折行不再叠成零行距的实心块。
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
        if (streakDays >= 2) {
            BadgeChip(text = "连续 $streakDays 天 🏅", spec = spec)
        }
    }
}
