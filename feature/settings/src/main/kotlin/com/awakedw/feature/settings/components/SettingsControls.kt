package com.awakedw.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.ThemeById
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.ThemeLaceOverlay
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.designsystem.onPrimarySurface
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.settings.SettingsValidation
import java.util.Locale

/** 步进器圆形小按钮直径。 */
private val STEPPER_BUTTON_SIZE = 48.dp

/** 间隔档位 chip 的最小宽度（P1-6）：不再 weight 均分，宽度自适应且不低于此值保住「120」等三位数。 */
private val INTERVAL_CHIP_MIN_WIDTH = 56.dp

/** 步进器小按钮形状：全圆。 */
private val STEP_BUTTON_SHAPE: Shape = RoundedCornerShape(14.dp)

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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = spec.greetingSubColor, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(valueText, color = spec.greetingColor, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            StepButton(text = "−", description = "减少$label", enabled = canDecrement, onClick = onDecrement, spec = spec)
            StepButton(text = "＋", description = "增加$label", enabled = canIncrement, onClick = onIncrement, spec = spec)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun StepButton(
    text: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    spec: ThemeSpec,
) {
    Surface(
        shape = STEP_BUTTON_SHAPE,
        color = spec.ringTrack.copy(alpha = if (enabled) 0.45f else 0.16f),
        modifier = Modifier.semantics { contentDescription = description },
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(STEPPER_BUTTON_SIZE)) {
            // P2-4：浅色主题主色底上白字对比不足，字色走 onPrimarySurface（深夜维持白字）。
            Text(text = text, color = spec.chipText.copy(alpha = if (enabled) 1f else 0.35f), style = MaterialTheme.typography.titleMedium)
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
                    checkedThumbColor = onPrimarySurface(spec),
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
@OptIn(ExperimentalMaterial3Api::class)
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("开始提醒", color = spec.greetingSubColor, style = MaterialTheme.typography.labelSmall)
                Text(formatWindowTime(range.start.toInt()), color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("结束提醒", color = spec.greetingSubColor, style = MaterialTheme.typography.labelSmall)
                Text(formatWindowTime(range.endInclusive.toInt()), color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
            }
        }
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
            startThumb = { ReminderSliderThumb() },
            endThumb = { ReminderSliderThumb() },
            track = {
                Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                    val min = SettingsValidation.WINDOW_MIN.toFloat()
                    val span = (SettingsValidation.WINDOW_MAX - SettingsValidation.WINDOW_MIN).toFloat()
                    val y = size.height / 2f
                    drawLine(spec.ringTrack, Offset(0f, y), Offset(size.width, y), strokeWidth = size.height, cap = StrokeCap.Round)
                    drawLine(
                        spec.primary,
                        Offset((range.start - min) / span * size.width, y),
                        Offset((range.endInclusive - min) / span * size.width, y),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round,
                    )
                }
            },
            colors =
                SliderDefaults.colors(
                    activeTrackColor = spec.primary,
                    inactiveTrackColor = spec.chipText.copy(alpha = 0.20f),
                    thumbColor = spec.primary,
                ),
        )
    }
}

/** Small pearl handle; RangeSlider retains native dragging, keyboard and accessibility behavior. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun ReminderSliderThumb() {
    val spec = currentThemeSpec()
    Box(Modifier.size(22.dp).shadow(2.dp, CircleShape).background(spec.chipBg, CircleShape).border(2.dp, spec.primary, CircleShape))
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

/** Theme choices shown as compact color cards instead of a form-like list. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun ThemeChoiceChips(
    selected: ThemeChoice,
    onSelect: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeChoiceCard(
            choice = ThemeChoice.FOLLOW_TIME,
            selected = selected == ThemeChoice.FOLLOW_TIME,
            onClick = { onSelect(ThemeChoice.FOLLOW_TIME) },
            modifier = Modifier.fillMaxWidth(),
        )
        ThemeChoice.entries.filter { it != ThemeChoice.FOLLOW_TIME }.chunked(2).forEach { choices ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                choices.forEach { choice ->
                    ThemeChoiceCard(
                        choice = choice,
                        selected = choice == selected,
                        onClick = { onSelect(choice) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (choices.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Theme option label. */
internal fun themeLabel(choice: ThemeChoice): String =
    when (choice) {
        ThemeChoice.FOLLOW_TIME -> "随时间"
        ThemeChoice.FIXED_EMERALD -> "晨雾蓝瓷"
        ThemeChoice.FIXED_STRAWBERRY -> "午后藕荷"
        ThemeChoice.FIXED_CARAMEL -> "黄昏奶茶"
        ThemeChoice.FIXED_NIGHT -> "深夜青黛"
        ThemeChoice.FIXED_LAVENDER -> "雾紫玫瑰"
        ThemeChoice.FIXED_GOTHIC -> "黑色哥特"
        ThemeChoice.FIXED_CLERIC -> "白色圣职"
        ThemeChoice.FIXED_THIN_MINT -> "薄荷巧克力"
    }

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ThemeChoiceCard(
    choice: ThemeChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = currentThemeSpec()
    val theme = if (choice == ThemeChoice.FOLLOW_TIME) current else ThemeById.getValue(themeIdOf(choice))
    val cardColor = if (selected) current.chipBg else current.chipBg.copy(alpha = 0.36f)
    val borderColor = if (selected) current.chipText.copy(alpha = 0.72f) else current.laceColor.copy(alpha = 0.42f)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(width = 1.dp, color = borderColor),
        onClick = onClick,
        selected = selected,
        modifier = modifier.heightIn(min = 86.dp).semantics { role = Role.RadioButton },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeSwatch(choice = choice, theme = theme, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = themeLabel(choice),
                    color = current.chipText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "已选择",
                        tint = current.chipText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun themeSwatchBrush(
    choice: ThemeChoice,
    theme: ThemeSpec,
): Brush =
    if (choice == ThemeChoice.FOLLOW_TIME) {
        Brush.horizontalGradient(
            listOf(
                ThemeById.getValue(ThemeId.STRAWBERRY).primary,
                ThemeById.getValue(ThemeId.EMERALD).primary,
                ThemeById.getValue(ThemeId.CARAMEL).primary,
                ThemeById.getValue(ThemeId.NIGHT).primary,
            ),
        )
    } else {
        Brush.linearGradient(theme.backgroundGradient)
    }

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ThemeSwatch(
    choice: ThemeChoice,
    theme: ThemeSpec,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier =
            modifier
                .heightIn(min = 54.dp)
                .background(themeSwatchBrush(choice, theme), shape)
                .clip(shape),
    ) {
        if (choice != ThemeChoice.FOLLOW_TIME) {
            val artwork = themeArtworkOf(theme.id)
            val image = rememberAssetImageOrN(artwork.asset, retainPreviousImage = false)
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    alpha = artwork.opacity.coerceAtLeast(0.48f),
                    modifier = Modifier.matchParentSize().testTag("theme-art-${theme.id.name}"),
                )
            }
            ThemeLaceOverlay(spec = theme, modifier = Modifier.matchParentSize(), compact = true)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(width = 30.dp, height = 14.dp)
                        .background(Brush.horizontalGradient(listOf(theme.buttonTop, theme.buttonBottom)), RoundedCornerShape(5.dp)),
            )
        }
        // A hairline highlight makes the swatch feel like a printed color card.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 1.dp)
                    .background(Color.White.copy(alpha = 0.24f), shape),
        )
    }
}

internal fun themeIdOf(choice: ThemeChoice): ThemeId =
    when (choice) {
        ThemeChoice.FIXED_EMERALD -> ThemeId.EMERALD
        ThemeChoice.FIXED_STRAWBERRY -> ThemeId.STRAWBERRY
        ThemeChoice.FIXED_CARAMEL -> ThemeId.CARAMEL
        ThemeChoice.FIXED_NIGHT -> ThemeId.NIGHT
        ThemeChoice.FIXED_LAVENDER -> ThemeId.LAVENDER
        ThemeChoice.FIXED_GOTHIC -> ThemeId.GOTHIC
        ThemeChoice.FIXED_CLERIC -> ThemeId.CLERIC
        ThemeChoice.FIXED_THIN_MINT -> ThemeId.THIN_MINT
        ThemeChoice.FOLLOW_TIME -> ThemeId.EMERALD
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
        selected = selected,
        modifier = modifier.heightIn(min = ControlMinHeight),
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
