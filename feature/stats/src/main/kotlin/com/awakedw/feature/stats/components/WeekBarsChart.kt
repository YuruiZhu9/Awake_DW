package com.awakedw.feature.stats.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.model.WeekBar
import com.awakedw.feature.stats.StatsMath

/** 柱状图绘图区高度。 */
private val CHART_HEIGHT = 160.dp

/** 柱宽占所在列槽位的比例，留出柱间呼吸感。 */
private const val BAR_WIDTH_FRACTION = 0.42f

/** 圆角顶半径占柱宽的比例。 */
private const val BAR_TOP_ROUND_FRACTION = 0.45f

/** 基线圆点半径。 */
private val BASELINE_DOT_RADIUS = 3.dp

/** 目标虚线宽。 */
private val GOAL_LINE_WIDTH = 1.5.dp

/** 目标虚线不透明度：给主色降一点存在感，别与达标柱抢戏。 */
private const val GOAL_LINE_ALPHA = 0.6f

/** 柱状图生长总时长（§10.3）：逐列错峰后单柱实际可见约 250ms。 */
private const val GROW_TOTAL_MS = 400

/** 逐列错峰步长（以生长总时长为 1 的比例）。 */
private const val STAGGER_FRACTION = 0.06f

/**
 * 本周柱状图（规格 §3.3 第 2 条）：近 7 天圆角顶柱 + 虚线目标线 + 末列「今」字标注。
 * 达标柱用主题 primary，其余柱与基线圆点用轨道色；柱高与目标线的几何换算见 [StatsMath]。
 * 入场编排（§10.3）：柱体自基线逐列生长、目标线与列标签随进度淡入——只播一次。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun WeekBarsChart(
    bars: List<WeekBar>,
    goalMl: Int,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    val values = bars.map { it.totalMl }
    val metGoals = StatsMath.metGoal(values, goalMl)
    val labels = StatsMath.columnLabels(bars.map { it.dayKey }, bars.lastOrNull()?.dayKey.orEmpty())

    val grow = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            grow.snapTo(1f)
        } else {
            grow.animateTo(1f, animationSpec = tween(durationMillis = GROW_TOTAL_MS, easing = EaseOutCubic))
        }
    }

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            val heights = StatsMath.barHeights(values, goalMl, size.height)
            val goalY = StatsMath.goalLineY(goalMl, values, size.height)
            val progress = grow.value
            drawGoalLine(goalY, spec.primary.copy(alpha = GOAL_LINE_ALPHA * progress))
            bars.forEachIndexed { index, _ ->
                val fraction = (progress - index * STAGGER_FRACTION).coerceIn(0f, 1f)
                val centerX = slotCenter(index, bars.size, size.width)
                when (val barHeight = heights[index] * fraction) {
                    0f -> drawBaselineDot(centerX, size.height, spec.ringTrack.copy(alpha = fraction))
                    else ->
                        drawBar(
                            centerX = centerX,
                            barHeight = barHeight,
                            slotWidth = size.width / bars.size,
                            color = if (metGoals[index]) spec.primary else spec.ringTrack,
                        )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    // 今天一列用主色点亮，与达标柱同色呼应。
                    color = if (index == labels.lastIndex) spec.primary else spec.greetingSubColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** 列槽位中心 x 坐标：绘图区宽度按列数均分。 */
private fun slotCenter(
    index: Int,
    columnCount: Int,
    totalWidth: Float,
): Float {
    val slotWidth = totalWidth / columnCount.coerceAtLeast(1)
    return slotWidth * index + slotWidth / 2f
}

private fun DrawScope.drawGoalLine(
    y: Float,
    color: Color,
) {
    val stroke = GOAL_LINE_WIDTH.toPx()
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = stroke,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 7f, stroke * 5f)),
    )
}

private fun DrawScope.drawBaselineDot(
    centerX: Float,
    chartHeight: Float,
    color: Color,
) {
    val radius = BASELINE_DOT_RADIUS.toPx()
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, chartHeight - radius),
    )
}

private fun DrawScope.drawBar(
    centerX: Float,
    barHeight: Float,
    slotWidth: Float,
    color: Color,
) {
    val barWidth = slotWidth * BAR_WIDTH_FRACTION
    val topRadius = CornerRadius(barWidth * BAR_TOP_ROUND_FRACTION)
    val rect =
        RoundRect(
            rect = Rect(offset = Offset(centerX - barWidth / 2f, size.height - barHeight), size = Size(barWidth, barHeight)),
            topLeft = topRadius,
            topRight = topRadius,
            bottomLeft = CornerRadius.Zero,
            bottomRight = CornerRadius.Zero,
        )
    drawPath(Path().apply { addRoundRect(rect) }, color = color)
}
