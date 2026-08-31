package com.awakedw.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeById
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.settings.SettingsValidation
import java.util.Locale

/** 步进器圆形小按钮直径。 */
private val STEPPER_BUTTON_SIZE = 32.dp

/** 主题色圆点直径。 */
private val THEME_DOT_SIZE = 14.dp

/** 步进器小按钮形状：全圆。 */
private val STEP_BUTTON_SHAPE: Shape = CircleShape

/** 选择 chips 的胶囊圆角：全圆。 */
private val CHIP_SHAPE: Shape = RoundedCornerShape(percent = 50)

/**
 * 「目标」区步进器行（§3.4）：标签 + 「− 数值 ＋」。
 * 点击 ± 由调用方提交 `当前值 ± [SettingsValidation.ML_STEP]`，
 * 越界候选交给 VM 校验回落——本层只做按钮可用性提示，不做夹紧。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun StepperRow(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = spec.chipText, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepButton(text = "−", enabled = canDecrement, onClick = onDecrement, spec = spec)
            Text(
                text = valueText,
                color = spec.greetingColor,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            StepButton(text = "＋", enabled = canIncrement, onClick = onIncrement, spec = spec)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun StepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    spec: ThemeSpec,
) {
    Surface(
        shape = STEP_BUTTON_SHAPE,
        color = if (enabled) spec.primary else spec.primary.copy(alpha = 0.30f),
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(STEPPER_BUTTON_SIZE)) {
            Text(text = text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** 提醒总开关行（§3.4）：标签 + Switch。切换即持久化，无保存键。 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = spec.chipText, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = spec.primary,
                    uncheckedThumbColor = spec.chipText,
                    uncheckedTrackColor = spec.chipText.copy(alpha = 0.20f),
                ),
        )
    }
}

/**
 * 清醒时段双滑杆（§3.4）：顶部「开始 HH:mm — 结束 HH:mm」标签 + 15 分钟粒度的双柄滑杆。
 * 拖动时本层保证两柄间隔 ≥ 45 分钟（15min 粒度下满足 start < end−30 的最小档），
 * 松手后按分钟数提交——VM 仍会再校验一次，双重兜底。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun WindowRangeSlider(
    startMin: Int,
    endMin: Int,
    onCommit: (startMin: Int, endMin: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    // 300..1380 共 73 个 15 分钟刻度：RangeSlider 的 steps = 刻度间隔数 − 1。
    val gridCount = (SettingsValidation.WINDOW_MAX - SettingsValidation.WINDOW_MIN) / SettingsValidation.WINDOW_GRANULARITY_MIN
    val steps = gridCount - 1
    // 粒度下可持久化的最小间隔：30 分钟下限向上取一个 15 分钟刻度。
    val gapGrids = SettingsValidation.WINDOW_GAP_MIN / SettingsValidation.WINDOW_GRANULARITY_MIN + 1
    val minGap = gapGrids * SettingsValidation.WINDOW_GRANULARITY_MIN
    var range by remember(startMin, endMin) { mutableStateOf(startMin.toFloat()..endMin.toFloat()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "开始 " + formatWindowTime(range.start.toInt()) + " — 结束 " + formatWindowTime(range.endInclusive.toInt()),
            color = spec.greetingColor,
            style = MaterialTheme.typography.titleSmall,
        )
        RangeSlider(
            value = range,
            onValueChange = { next ->
                val pulledStart =
                    if (next.start != range.start) {
                        minOf(next.start, next.endInclusive - minGap)
                    } else {
                        next.start
                    }
                val pulledEnd = maxOf(next.endInclusive, pulledStart + minGap)
                range = pulledStart..pulledEnd
            },
            onValueChangeFinished = { onCommit(range.start.toInt(), range.endInclusive.toInt()) },
            valueRange = SettingsValidation.WINDOW_MIN.toFloat()..SettingsValidation.WINDOW_MAX.toFloat(),
            steps = steps,
            colors =
                SliderDefaults.colors(
                    activeTrackColor = spec.primary,
                    inactiveTrackColor = spec.chipText.copy(alpha = 0.20f),
                    thumbColor = spec.primary,
                ),
        )
    }
}

/** 提醒间隔档位 chips（§3.4）：候选来自 [SettingsValidation.INTERVAL_CHOICES]，点选即时生效。 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun IntervalChipsRow(
    selectedMin: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsValidation.INTERVAL_CHOICES.forEach { min ->
            SelectableChip(
                label = "$min",
                selected = min == selectedMin,
                onClick = { onSelect(min) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 外观主题单选 chips（§3.4）：「跟随时间 / 固定翡翠绿 / 固定草莓雾光 / 固定焦糖奶茶 / 固定深夜墨青」，
 * 每个选项带色点——跟随时间为四主题色渐变点，固定项为对应主题主色实心点。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun ThemeChoiceChips(
    selected: ThemeChoice,
    onSelect: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeChoice.entries.forEach { choice ->
            SelectableChip(
                label = themeLabel(choice),
                selected = choice == selected,
                onClick = { onSelect(choice) },
                modifier = Modifier.fillMaxWidth(),
                leading = { ThemeDot(choice) },
            )
        }
    }
}

/** 主题选项文案（§3.4 原文 + §10.1 深夜墨青）。 */
internal fun themeLabel(choice: ThemeChoice): String =
    when (choice) {
        ThemeChoice.FOLLOW_TIME -> "跟随时间"
        ThemeChoice.FIXED_EMERALD -> "固定翡翠绿"
        ThemeChoice.FIXED_STRAWBERRY -> "固定草莓雾光"
        ThemeChoice.FIXED_CARAMEL -> "固定焦糖奶茶"
        ThemeChoice.FIXED_NIGHT -> "固定深夜墨青"
    }

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ThemeDot(choice: ThemeChoice) {
    val brush =
        when (choice) {
            ThemeChoice.FOLLOW_TIME ->
                Brush.horizontalGradient(
                    listOf(
                        ThemeById.getValue(ThemeId.STRAWBERRY).primary,
                        ThemeById.getValue(ThemeId.EMERALD).primary,
                        ThemeById.getValue(ThemeId.CARAMEL).primary,
                        ThemeById.getValue(ThemeId.NIGHT).primary,
                    ),
                )
            else -> Brush.horizontalGradient(listOf(themePrimary(choice), themePrimary(choice)))
        }
    Box(modifier = Modifier.size(THEME_DOT_SIZE).background(brush, CircleShape))
}

private fun themePrimary(choice: ThemeChoice): Color =
    when (choice) {
        ThemeChoice.FIXED_EMERALD -> ThemeById.getValue(ThemeId.EMERALD).primary
        ThemeChoice.FIXED_STRAWBERRY -> ThemeById.getValue(ThemeId.STRAWBERRY).primary
        ThemeChoice.FIXED_CARAMEL -> ThemeById.getValue(ThemeId.CARAMEL).primary
        ThemeChoice.FIXED_NIGHT -> ThemeById.getValue(ThemeId.NIGHT).primary
        ThemeChoice.FOLLOW_TIME -> ThemeById.getValue(ThemeId.EMERALD).primary
    }

@Suppress("ktlint:standard:function-naming")
@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val spec = currentThemeSpec()
    Surface(
        shape = CHIP_SHAPE,
        color = if (selected) spec.primary else spec.chipText.copy(alpha = 0.10f),
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                color = if (selected) Color.White else spec.chipText,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

/** 分钟数 → 「HH:mm」（如 480 → "08:00"）。 */
internal fun formatWindowTime(minOfDay: Int): String = String.format(Locale.US, "%02d:%02d", minOfDay / 60, minOfDay % 60)
