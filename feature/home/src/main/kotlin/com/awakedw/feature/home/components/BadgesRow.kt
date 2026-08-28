package com.awakedw.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec

/** 徽章胶囊圆角：全圆。 */
private val CHIP_SHAPE: Shape = RoundedCornerShape(percent = 50)

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

@Suppress("ktlint:standard:function-naming")
@Composable
private fun BadgeChip(
    text: String,
    spec: ThemeSpec,
) {
    Surface(shape = CHIP_SHAPE, color = spec.chipBg) {
        Text(
            text = text,
            color = spec.chipText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}
