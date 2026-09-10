package com.awakedw.feature.stats.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.WaterRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 时间线空态占位高度：让「还没出现」的文案有一块安静的居中空间。 */
private val EMPTY_TIMELINE_HEIGHT = 76.dp

/** 今日尚无记录时的空态文案：陈述事实，不做催促，也不写成格言。 */
internal const val EMPTY_TIMELINE_TEXT = "今天还没有记录，第一杯随时可以开始"

/** 纠错手势的说明行：长按删除是隐藏手势，必须写出来。 */
internal const val DELETE_HINT_TEXT = "长按某一条可以删除它"

/** 删除动作的无障碍标签，长按与屏幕阅读器共用同一措辞。 */
internal const val DELETE_ACTION_LABEL = "删除这条记录"

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
 *
 * [onRequestDelete] 非空时每条可长按发起删除（由调用方弹确认框）：
 * 记错的一杯要能收回，纠错入口就放在对应那一行上。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun TodayTimeline(
    records: List<WaterRecord>,
    modifier: Modifier = Modifier,
    onRequestDelete: ((WaterRecord) -> Unit)? = null,
) {
    if (records.isEmpty()) {
        FadeUpOnce(delayMillis = 120, modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = EMPTY_TIMELINE_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = EMPTY_TIMELINE_TEXT,
                    color = currentThemeSpec().greetingSubColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
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
                    TimelineRow(record = record, spec = spec, onRequestDelete = onRequestDelete)
                }
            }
            if (isCollapsible) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = ControlMinHeight)
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
            // 长按是隐藏手势，用一行小字把它说出来，避免只有开发者知道。
            if (onRequestDelete != null) {
                Text(
                    text = DELETE_HINT_TEXT,
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun TimelineRow(
    record: WaterRecord,
    spec: ThemeSpec,
    onRequestDelete: ((WaterRecord) -> Unit)?,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val time = TIME_FORMAT.format(Instant.ofEpochMilli(record.drankAtEpochMs).atZone(ZoneId.systemDefault()))
    val longPressModifier =
        if (onRequestDelete == null) {
            Modifier
        } else {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClickLabel = DELETE_ACTION_LABEL,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onRequestDelete(record)
                },
            )
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ControlMinHeight)
                .then(longPressModifier)
                .background(spec.ringTrack.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                .semantics {
                    contentDescription =
                        if (onRequestDelete == null) {
                            "$time，${record.amountMl}ml"
                        } else {
                            "$time，${record.amountMl}ml，长按可$DELETE_ACTION_LABEL"
                        }
                    if (onRequestDelete != null) {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(DELETE_ACTION_LABEL) {
                                    onRequestDelete(record)
                                    true
                                },
                            )
                    }
                }
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
            text = time,
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
