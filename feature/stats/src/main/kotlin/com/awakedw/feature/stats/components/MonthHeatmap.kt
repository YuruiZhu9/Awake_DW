package com.awakedw.feature.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.onPrimarySurface
import com.awakedw.feature.stats.ArchiveMath
import com.awakedw.feature.stats.MonthCell

/** 热力格圆角：与快捷胶囊、周柱顶同族的小圆角。 */
private val HEAT_CELL_SHAPE = RoundedCornerShape(7.dp)

/** 热力五档透明度（视觉基线 §18）：0 档由轨道色承担，1–4 档压在主色上。 */
private val HEAT_ALPHAS = listOf(0f, 0.20f, 0.42f, 0.66f, 1f)

/** 数字改用主表面色的起始档位：≥0.66 的主色底上，纸面字更可读。 */
private const val ON_PRIMARY_HEAT_LEVEL = 3

/** 图例小样的边长与「少/多」之间的梯度块数。 */
private val LEGEND_SWATCH_SIZE = 10.dp
private const val LEGEND_SWATCH_COUNT = 5

/**
 * 本月热力（2.1.0 档案化，Zero 式月历）：周一首行的月历网格，
 * 每格深浅 = 当日总量距目标的完成度（[ArchiveMath.heatLevel] 五档主色），
 * 今天用主色描边圈出，未来日期淡显留空——不是缺数据，是还没发生。
 * 容器整体一条摘要语义（[summary]），TalkBack 不逐格走 31 个数字。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun MonthHeatmap(
    cells: List<MonthCell?>,
    goalMl: Int,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = summary },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        cells.chunked(7).forEach { week ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { cell ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell != null) HeatCell(cell, goalMl, spec)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("少", color = spec.greetingSubColor, style = MaterialTheme.typography.labelSmall)
            repeat(LEGEND_SWATCH_COUNT) { index ->
                Box(
                    Modifier
                        .size(LEGEND_SWATCH_SIZE)
                        .clip(HEAT_CELL_SHAPE)
                        .background(
                            when (index) {
                                0 -> spec.ringTrack.copy(alpha = 0.55f)
                                else -> spec.primary.copy(alpha = HEAT_ALPHAS[index + 1])
                            },
                        ),
                )
            }
            Text("多", color = spec.greetingSubColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun HeatCell(
    cell: MonthCell,
    goalMl: Int,
    spec: ThemeSpec,
) {
    val level = if (cell.isFuture) 0 else ArchiveMath.heatLevel(cell.totalMl, goalMl)
    val background =
        when {
            cell.isFuture -> spec.ringTrack.copy(alpha = 0.12f)
            level == 0 -> spec.ringTrack.copy(alpha = 0.30f)
            else -> spec.primary.copy(alpha = HEAT_ALPHAS[level])
        }
    val numberColor =
        when {
            cell.isFuture -> spec.greetingSubColor.copy(alpha = 0.4f)
            level >= ON_PRIMARY_HEAT_LEVEL -> onPrimarySurface(spec)
            else -> spec.greetingColor
        }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(HEAT_CELL_SHAPE)
                .background(background)
                .then(if (cell.isToday) Modifier.border(1.2.dp, spec.primary, HEAT_CELL_SHAPE) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cell.dayNumber.toString(),
            color = numberColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
