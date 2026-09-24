package com.awakedw.feature.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.feature.home.RingNote
import java.time.DayOfWeek
import java.time.LocalDateTime

/** The compact spacing contract used by the 2.0 editorial home composition. */
val EDITORIAL_HOME_TOP_PADDING = 22.dp
val EDITORIAL_HERO_GAP = 18.dp
val EDITORIAL_MASTHEAD_ESTIMATE = 60.dp

/** Stable Chinese weekday labels for the date masthead. */
internal fun editorialDateLabel(dateTime: LocalDateTime): String {
    val weekday =
        when (dateTime.dayOfWeek) {
            DayOfWeek.MONDAY -> "星期一"
            DayOfWeek.TUESDAY -> "星期二"
            DayOfWeek.WEDNESDAY -> "星期三"
            DayOfWeek.THURSDAY -> "星期四"
            DayOfWeek.FRIDAY -> "星期五"
            DayOfWeek.SATURDAY -> "星期六"
            DayOfWeek.SUNDAY -> "星期日"
        }
    return "${dateTime.monthValue}月${dateTime.dayOfMonth}日 · $weekday"
}

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

/** Left-aligned editorial masthead: date as a刊头, greeting as the page's opening line. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun EditorialHomeMasthead(
    customGreeting: String?,
    modifier: Modifier = Modifier,
) {
    val now = LocalDateTime.now()
    val spec = currentThemeSpec()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = editorialDateLabel(now),
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
        )
        Text(
            text = customGreeting ?: greetingFor(com.awakedw.core.common.TimeSlots.slotOfHour(now.hour)),
            color = spec.greetingColor,
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
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
    val reduceMotion = rememberReduceMotion()
    val rolledTotal =
        if (reduceMotion) {
            totalMl
        } else {
            animateIntAsState(
                targetValue = totalMl,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "editorialTotalMl",
            ).value
        }
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
        )
        Text(
            text = editorialGoalLine(totalMl, goalMl),
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.3.sp),
        )
    }
}

/** Compact ring center: progress is the instrument; praise/revert copy still owns the ring center. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun CompactRingCenterContent(
    progress: Float,
    centerNote: RingNote?,
    spec: ThemeSpec = currentThemeSpec(),
) {
    if (centerNote == null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                color = spec.ringValueText,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.width(1.dp))
            Text(
                text = "今日进度",
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
