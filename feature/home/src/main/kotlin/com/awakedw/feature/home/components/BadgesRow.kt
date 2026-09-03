package com.awakedw.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.currentThemeSpec

/**
 * 首页事实摘要：把原先三枚高频胶囊收敛成一张安静的纸面数据条。
 * 仍然只表达今日杯数、最近一杯和平均间隔，不承载连续、奖励或收藏语义。
 */
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = spec.chipBg.copy(alpha = 0.26f),
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = 0.34f)),
        tonalElevation = 0.dp,
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FactItem(
                label = "今日",
                value = "$cupCount 杯",
                description = "今日 $cupCount 杯",
            )
            if (lastDrinkLabel != null) {
                FactItem(
                    label = "最近一杯",
                    value = lastDrinkLabel,
                    description = "最近一杯 $lastDrinkLabel",
                )
            }
            FactItem(
                label = "平均间隔",
                value = avgIntervalLabel,
                description = "平均间隔 $avgIntervalLabel",
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun FactItem(
    label: String,
    value: String,
    description: String,
) {
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = currentThemeSpec().greetingSubColor,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
        )
        Text(
            text = value,
            color = currentThemeSpec().chipText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
