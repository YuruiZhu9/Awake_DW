package com.awakedw.feature.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.feature.stats.ArchiveMath
import com.awakedw.feature.stats.RhythmSlice

/** 堆叠条高度与段间纸缝：缝是纸面的呼吸，不是分隔线。 */
private val RHYTHM_BAR_HEIGHT = 14.dp
private val RHYTHM_GAP = 2.dp

/** 段圆角半径：小圆角胶囊段，与按钮/胶囊同族。 */
private val RHYTHM_SEGMENT_RADIUS = 4.dp

/** 早/白天/晚三段的主色透明度（视觉基线 §18）：单色渐层，不引入新色。 */
private val RHYTHM_ALPHAS = listOf(1f, 0.55f, 0.28f)

/** 图例色点直径。 */
private val LEGEND_DOT_SIZE = 7.dp

/**
 * 时段节律（2.1.0 档案化，Slopes 式堆叠分布条）：近七日早/白天/晚的构成，
 * 一条堆叠条 + 三行图例（色点 + 标签 + 右对齐毫升数）。毫升是事实，不放百分比；
 * 整段近七日零记录时由调用方整节静默，不画空条。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun RhythmBars(
    slices: List<RhythmSlice>,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Column(modifier = modifier) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(RHYTHM_BAR_HEIGHT)
                    .semantics { contentDescription = summary },
        ) {
            drawRhythm(slices, spec)
        }
        Spacer(Modifier.height(10.dp))
        slices.forEachIndexed { index, slice ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(LEGEND_DOT_SIZE)
                        .background(spec.primary.copy(alpha = RHYTHM_ALPHAS[index]), CircleShape),
                )
                Text(
                    text = ArchiveMath.slotLabel(slice.slot),
                    color = spec.greetingColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${slice.totalMl}ml",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun DrawScope.drawRhythm(
    slices: List<RhythmSlice>,
    spec: ThemeSpec,
) {
    val total = slices.sumOf { it.totalMl }
    if (total <= 0) return
    val gap = RHYTHM_GAP.toPx()
    val radius = RHYTHM_SEGMENT_RADIUS.toPx()
    val nonzero = slices.count { it.totalMl > 0 }
    val unit = (size.width - gap * (nonzero - 1)) / total
    var x = 0f
    slices.forEachIndexed { index, slice ->
        if (slice.totalMl <= 0) return@forEachIndexed
        val width = slice.totalMl * unit
        drawRoundRect(
            color = spec.primary.copy(alpha = RHYTHM_ALPHAS[index]),
            topLeft = Offset(x, 0f),
            size = Size(width, size.height),
            cornerRadius = CornerRadius(radius, radius),
        )
        x += width + gap
    }
}
