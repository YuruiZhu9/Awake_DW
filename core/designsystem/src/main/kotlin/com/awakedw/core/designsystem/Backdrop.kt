package com.awakedw.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// 统一质感参数（设计规格 §2.2）
private val GRAIN_CELL = 4.dp // 噪点网格间距：4px 微点纹理
private val GRAIN_DOT = 1.dp // 单颗微点直径
private const val GRAIN_ALPHA = 0.05f // 颗粒不透明度约 4–5%
private const val HALO_ALPHA = 0.32f // 柔光晕：主色低透明度
private val HALO_DIAMETER = 170.dp // 光晕团约 170px
private const val HALO_CENTER_Y_FRACTION = 0.34f // 首页进度环大致位于纵向 1/3 处

/**
 * 全局背景质感底座（规格 §2.1 渐变 + §2.2 质感手法）：
 * 背景渐变底 + 进度环背后柔光晕 + 全屏噪点颗粒层。
 *
 * 三层绘制在同一离屏图层内，颗粒用 multiply 混合压住大面积渐变的塑料感；
 * 噪点为确定性错位网格，不含随机数，规避重组抖动。漂浮粒子不在本底座内，
 * 由上层页面按 [ThemeSpec.particleColors] 另行绘制。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun GradientBackdrop(
    spec: ThemeSpec,
    modifier: Modifier,
) {
    val backdropModifier =
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val gradient = Brush.verticalGradient(spec.backgroundGradient)
                val haloCenter = Offset(size.width / 2f, size.height * HALO_CENTER_Y_FRACTION)
                val haloRadius = (HALO_DIAMETER / 2).toPx()
                val haloColors = listOf(spec.haloColor.copy(alpha = HALO_ALPHA), Color.Transparent)
                val haloBrush =
                    Brush.radialGradient(colors = haloColors, center = haloCenter, radius = haloRadius)
                val grain = buildGrainPath(size, GRAIN_CELL.toPx(), GRAIN_DOT.toPx())
                onDrawBehind {
                    drawRect(gradient)
                    drawCircle(brush = haloBrush, radius = haloRadius, center = haloCenter)
                    drawPath(grain, color = Color.Black, alpha = GRAIN_ALPHA, blendMode = BlendMode.Multiply)
                }
            }

    Box(modifier = backdropModifier)
}

/** 确定性构建 4px 错位网格的微点路径；尺寸变化时随 drawWithCache 重算一次。 */
private fun buildGrainPath(
    bounds: Size,
    cellPx: Float,
    dotPx: Float,
): Path =
    Path().apply {
        var row = 0
        var y = cellPx / 2f
        while (y < bounds.height + dotPx) {
            val rowOffset = if (row % 2 == 0) 0f else cellPx / 2f
            var x = cellPx / 2f + rowOffset
            while (x < bounds.width + dotPx) {
                addOval(Rect(center = Offset(x, y), radius = dotPx / 2f))
                x += cellPx
            }
            y += cellPx
            row++
        }
    }
