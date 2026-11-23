package com.awakedw.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.animation.rememberRollingInt
import com.awakedw.core.designsystem.components.editorialDateLabel
import com.awakedw.core.designsystem.components.editorialFolio
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberUiNow
import com.awakedw.feature.home.RingNote
import java.time.LocalDateTime

/** The compact spacing contract used by the 2.0 editorial home composition. */
val EDITORIAL_HOME_TOP_PADDING = 22.dp
val EDITORIAL_HERO_GAP = 18.dp
val EDITORIAL_MASTHEAD_ESTIMATE = 60.dp

/** 刊头→英雄行：与 SplashMorph 交棒落点估算里的 +10dp 对齐（2.0.3 一屏收敛）。 */
val EDITORIAL_MASTHEAD_TO_HERO_GAP = 10.dp

/** 英雄行→主按钮：保持被几何断言守护的贴身节奏（0..18dp 区间）。 */
val EDITORIAL_HERO_TO_ACTION_GAP = 10.dp

/** Factual goal line beneath the hero value; it never implies a reward or streak. */
internal fun editorialGoalLine(
    totalMl: Int,
    goalMl: Int,
): String =
    if (totalMl >= goalMl) {
        "每日目标 ${goalMl}ml · 已达成"
    } else {
        "每日目标 ${goalMl}ml · 还差 ${goalMl - totalMl}ml"
    }

/**
 * Left-aligned editorial masthead: date + folio as the page header, a full-width hairline,
 * greeting as the page's opening line.
 *
 * 版式开销：日期行与问候之间只加 4dp + 1dp 线 + 7dp——约 12dp，仍守住
 * 360dp × 520dp 大字体一屏读完的既有门禁。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun EditorialHomeMasthead(
    customGreeting: String?,
    modifier: Modifier = Modifier,
    now: LocalDateTime = rememberUiNow(),
) {
    val spec = currentThemeSpec()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = editorialDateLabel(now),
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = editorialFolio(now),
                color = spec.greetingSubColor.copy(alpha = 0.85f),
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                    ),
                modifier = Modifier.alignByBaseline().testTag("home-folio"),
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(spec.greetingSubColor.copy(alpha = 0.40f)),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = customGreeting ?: greetingFor(com.awakedw.core.common.TimeSlots.slotOfHour(now.hour)),
            color = spec.greetingColor,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            textAlign = TextAlign.Start,
        )
    }
}

/** The new home hero value: the number is the page anchor, no longer trapped inside the ring. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun EditorialHeroValue(
    totalMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val rolledTotal = rememberRollingInt(targetValue = totalMl, label = "editorialTotalMl")
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "今日饮水",
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.4.sp),
        )
        Text(
            text =
                buildAnnotatedString {
                    append(rolledTotal.toString())
                    withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)) { append("ml") }
                },
            color = spec.ringValueText,
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
            // 英雄数字的几何回归锚点：光环等环内装饰不得把权重列挤成 0 宽。
            modifier = Modifier.testTag("home-hero-number"),
        )
        Text(
            text = editorialGoalLine(totalMl, goalMl),
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.3.sp),
        )
    }
}

/** Compact ring center: progress is the instrument; drag preview, praise and revert copy own it in turn. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun CompactRingCenterContent(
    progress: Float,
    centerNote: RingNote?,
    dragPreviewMl: Int? = null,
    showDragHint: Boolean = false,
    spec: ThemeSpec = currentThemeSpec(),
) {
    if (dragPreviewMl != null) {
        // 拖环预览临时态（2.2.0 环即把手）：拖动中环心跟手陈述事实，不落库；松手由提交/取消收场。
        // 窗环（Phase 1）把字号收到 titleMedium：88dp 圆牌在大字体下也完整容得下。
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+${dragPreviewMl}ml",
                color = spec.ringValueText,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.width(1.dp))
            Text(
                text = "松手记录",
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            )
        }
    } else if (centerNote == null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                color = spec.ringValueText,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.width(1.dp))
            Text(
                // 空态才有这一次轻提示：告诉使用者环也能拖（2.10.0）；有记录后回到「今日进度」。
                text = if (showDragHint) "可拖动记录" else "今日进度",
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Text(
                text = centerNote.text,
                color = spec.chipText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                textAlign = TextAlign.Center,
            )
            if (centerNote.attribution != null) {
                Spacer(Modifier.width(1.dp))
                Text(
                    text = "—— ${centerNote.attribution}",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.sp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
