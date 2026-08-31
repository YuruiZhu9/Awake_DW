package com.awakedw.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.particles.GRAIN_CELL
import com.awakedw.core.designsystem.particles.GRAIN_DOT
import com.awakedw.core.designsystem.particles.buildGrainPath

// 统一质感参数（设计规格 §2.2）；噪点网格常量沉淀于 particles 包共用。
private const val GRAIN_ALPHA = 0.05f // 颗粒不透明度约 4–5%
private const val HALO_ALPHA = 0.32f // 柔光晕：主色低透明度
private val HALO_DIAMETER = 170.dp // 光晕团约 170px
private const val HALO_CENTER_Y_FRACTION = 0.34f // 首页进度环大致位于纵向 1/3 处

/** 光晕呼吸（设计 §9.5）：alpha 在基准上下的摆幅。 */
private const val HALO_BREATH_AMPLITUDE = 0.07f

/** 光晕呼吸单程时长（ms）：约 8s 一个完整周期，慢到近乎察觉不到。 */
private const val HALO_BREATH_PERIOD_MS = 8_000L

/**
 * 全局背景质感底座（规格 §2.1 渐变 + §2.2 质感手法）：
 * 背景渐变底 + 进度环背后柔光晕 + 全屏噪点颗粒层。
 *
 * 三层绘制在同一离屏图层内，颗粒用 multiply 混合压住大面积渐变的塑料感；
 * 噪点为确定性错位网格，不含随机数，规避重组抖动。柔光晕带约 8s 周期的
 * 轻呼吸（仅透明度正弦摆动，布局与 Brush 均不重建），给界面一层底色上的呼吸感。
 * 漂浮粒子不在本底座内，由上层页面按 [ThemeSpec.particleColors] 另行绘制。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun GradientBackdrop(
    spec: ThemeSpec,
    modifier: Modifier,
) {
    // 呼吸相位 -1..1：只进 draw 相，不触发重组/重缓存。
    val breath = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            last = now
            val t = (now % (HALO_BREATH_PERIOD_MS * 1_000_000L)).toFloat() / (HALO_BREATH_PERIOD_MS * 1_000_000L)
            breath.floatValue = kotlin.math.sin(t * 2f * kotlin.math.PI.toFloat())
        }
    }

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
                    drawCircle(
                        brush = haloBrush,
                        radius = haloRadius,
                        center = haloCenter,
                        alpha = 1f + HALO_BREATH_AMPLITUDE * breath.floatValue,
                    )
                    drawPath(grain, color = Color.Black, alpha = GRAIN_ALPHA, blendMode = BlendMode.Multiply)
                }
            }

    Box(modifier = backdropModifier)
}
