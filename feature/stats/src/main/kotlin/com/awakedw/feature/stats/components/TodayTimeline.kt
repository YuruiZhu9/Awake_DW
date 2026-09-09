package com.awakedw.feature.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.WaterRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 时间线空态占位高度：让「还没出现」的文案有一块安静的居中空间。 */
private val EMPTY_TIMELINE_HEIGHT = 76.dp

/** 折叠时间线保留的最近记录数，避免长列表把统计页拉得过长。 */
private const val COLLAPSED_RECORD_LIMIT = 5

/** 小水滴圆点直径。 */
private val DROP_DOT_SIZE = 8.dp

/** 行与行之间的呼吸间距。 */
private val ROW_SPACING = 8.dp

/** 逐条入场的错峰步长（§10.3）：前若干行依次晚 40ms，长列表不再累积等待。 */
private const val ROW_ENTRANCE_STAGGER_MS = 40

/** 参与错峰的最大行数：其后的行同刻入场。 */
private const val ROW_ENTRANCE_MAX_STAGGERED = 6

/** HH:mm 展示格式（显示层专用，随系统时区）。 */
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 今日时间线（规格 §3.3 第 3 条）：每杯一个小水滴圆点 + HH:mm，右侧「{ml}ml」，时间升序。
 * 今日一杯未喝时整块居中显示空态文案；超过五条时默认只保留最近五条，点击后可展开。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun TodayTimeline(
    records: List<WaterRecord>,
    modifier: Modifier = Modifier,
) {
    if (records.isEmpty()) {
        FadeUpOnce(delayMillis = 120, modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = EMPTY_TIMELINE_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "今天还没有饮水记录",
                    color = currentThemeSpec().greetingSubColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        val spec = currentThemeSpec()
        var expanded by remember { mutableStateOf(false) }
        val isCollapsible = records.size > COLLAPSED_RECORD_LIMIT
        val visibleRecords =
            if (isCollapsible && !expanded) {
                records.takeLast(COLLAPSED_RECORD_LIMIT)
            } else {
                records
            }

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            visibleRecords.forEachIndexed { index, record ->
                FadeUpOnce(delayMillis = minOf(index, ROW_ENTRANCE_MAX_STAGGERED) * ROW_ENTRANCE_STAGGER_MS) {
                    TimelineRow(record = record, spec = spec)
                }
            }
            if (isCollapsible) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription =
                                    if (expanded) {
                                        "收起今日记录"
                                    } else {
                                        "显示其余 ${records.size - COLLAPSED_RECORD_LIMIT} 次今日记录"
                                    }
                            },
                ) {
                    Text(
                        text =
                            if (expanded) {
                                "收起"
                            } else {
                                "显示更多（还有 ${records.size - COLLAPSED_RECORD_LIMIT} 次）"
                            },
                        color = spec.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun TimelineRow(
    record: WaterRecord,
    spec: ThemeSpec,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(spec.ringTrack.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(DROP_DOT_SIZE)
                    .background(color = spec.primary, shape = CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = TIME_FORMAT.format(Instant.ofEpochMilli(record.drankAtEpochMs).atZone(ZoneId.systemDefault())),
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${record.amountMl}ml",
            color = spec.greetingColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
