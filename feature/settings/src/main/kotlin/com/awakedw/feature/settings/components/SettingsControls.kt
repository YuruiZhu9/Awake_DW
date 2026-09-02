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
import androidx.compose.foundation.layout.widthIn
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
import com.awakedw.core.designsystem.onPrimarySurface
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.settings.SettingsValidation
import java.util.Locale

/** 步进器圆形小按钮直径。 */
private val STEPPER_BUTTON_SIZE = 32.dp

/** 间隔档位 chip 的最小宽度（P1-6）：不再 weight 均分，宽度自适应且不低于此值保住「120」等三位数。 */
private val INTERVAL_CHIP_MIN_WIDTH = 56.dp

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
            // P2-4：浅色主题主色底上白字对比不足，字色走 onPrimarySurface（深夜维持白字）。
            Text(text = text, color = onPrimarySurface(spec), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * 开关行（§3.4）：标签（可选副文案 [supporting]）+ Switch。切换即持久化，无保存键。
 * 副文案收在标签下方的小字（greetingSubColor），如「音效 · 水滴与八音盒；系统静音时自动安静」。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val spec = currentThemeSpec()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = spec.chipText, style = MaterialTheme.typography.bodyMedium)
            if (supporting != null) {
                Text(
                    text = supporting,
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
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
 * P3-4：steps=0 去掉 71 档刻度噪点，15 分钟取整改在值变化处手动 snap
 * （[SettingsValidation.snapToWindowGranularity]），落点行为与旧刻度档一致、视觉干净。
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
                // steps=0 后刻度停靠由本层自理：先吸附 15 分钟粒度，再维持最小间隔。
                val snapped =
                    SettingsValidation.snapToWindowGranularity(next.start)
                        .toFloat()..SettingsValidation.snapToWindowGranularity(next.endInclusive).toFloat()
                val pulledStart =
                    if (snapped.start != range.start) {
                        minOf(snapped.start, snapped.endInclusive - minGap)
                    } else {
                        snapped.start
                    }
                val pulledEnd = maxOf(snapped.endInclusive, pulledStart + minGap)
                range = pulledStart..pulledEnd
            },
            onValueChangeFinished = { onCommit(range.start.toInt(), range.endInclusive.toInt()) },
            valueRange = SettingsValidation.WINDOW_MIN.toFloat()..SettingsValidation.WINDOW_MAX.toFloat(),
            steps = 0,
            colors =
                SliderDefaults.colors(
                    activeTrackColor = spec.primary,
                    inactiveTrackColor = spec.chipText.copy(alpha = 0.20f),
                    thumbColor = spec.primary,
                ),
        )
    }
}

/**
 * 提醒间隔档位 chips（§3.4）：候选来自 [SettingsValidation.INTERVAL_CHOICES]，点选即时生效。
 * P1-6：七档按 4+3 拆两行——原不换行 Row + `weight(1f)` 均分在 360dp 屏每格仅 ≈33dp，
 * 「120」以上右半被裁；改为 [INTERVAL_CHIP_MIN_WIDTH] 定宽下限、不参与均分，行内/行间 8dp 呼吸。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun IntervalChipsRow(
    selectedMin: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsValidation.INTERVAL_CHOICES.chunked(4).forEach { rowChoices ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowChoices.forEach { min ->
                    SelectableChip(
                        label = "$min",
                        selected = min == selectedMin,
                        onClick = { onSelect(min) },
                        modifier = Modifier.widthIn(min = INTERVAL_CHIP_MIN_WIDTH),
                    )
                }
            }
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
                // P2-4：选中 chip 主色底上的字色走 onPrimarySurface（深夜维持白字）。
                color = if (selected) onPrimarySurface(spec) else spec.chipText,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

/** 分钟数 → 「HH:mm」（如 480 → "08:00"）。 */
internal fun formatWindowTime(minOfDay: Int): String = String.format(Locale.US, "%02d:%02d", minOfDay / 60, minOfDay % 60)
