package com.awakedw.core.designsystem.lolita

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.designsystem.scene.rememberSceneSpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 近景失焦光斑颗数：少而大，只存在于视野边缘。 */
private const val BOKEH_COUNT = 10

/** 近景层漂移相位循环：120s，与中景层（90s）错开节奏。 */
private const val BOKEH_LOOP_NANOS = 120_000_000_000L

/** 中央避让带：光斑锚点必须落在 |x-0.5|≥0.30 或 |y-0.5|≥0.32 的边缘环带内。 */
private const val CENTER_CLEAR_X = 0.30f
private const val CENTER_CLEAR_Y = 0.32f

/**
 * 近景失焦层（alpha13 基线 §13 场景第三层）：大而柔的失焦光斑，纯代码绘制——
 * 不依赖素材，八主题通用；颜色取自各主题 [com.awakedw.core.designsystem.ThemeSpec.particleColors]，
 * 因此光斑天然随主题换色。
 *
 * 锚点由 `(seed, index)` 确定性推导并做中央避让（拒绝式重采样），运动学为
 * 低频正弦相位（只重绘不重组）；透明度随时段氛围（[rememberSceneSpec]）插值，
 * 减少动态下完全静止。层不进语义树、不承载业务语义。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun BokehLayer(
    modifier: Modifier = Modifier,
    seed: Long = 23L,
) {
    val colors = currentThemeSpec().particleColors
    val scene = rememberSceneSpec()
    val reduceMotion = rememberReduceMotion()
    val progress = remember { mutableFloatStateOf(0f) }
    if (!reduceMotion) {
        LaunchedEffect(seed) {
            var last = withFrameNanos { it }
            var accumulated = 0L
            while (true) {
                val now = withFrameNanos { it }
                accumulated += now - last
                last = now
                progress.floatValue = (accumulated % BOKEH_LOOP_NANOS).toFloat() / BOKEH_LOOP_NANOS
            }
        }
    }

    Box(
        modifier =
            modifier.drawWithCache {
                val palette = if (colors.isEmpty()) listOf(FALLBACK_BOKEH_COLOR) else colors
                val anchors =
                    List(BOKEH_COUNT) { index ->
                        bokehAnchor(seed, index, size.width, size.height, palette)
                    }
                onDrawBehind {
                    val p = progress.floatValue
                    val twoPi = (2.0 * PI).toFloat()
                    val boost = scene.particleBoost
                    anchors.forEachIndexed { index, anchor ->
                        val phase = index * 0.9f
                        val driftX = anchor.radiusPx * 0.35f * sin(twoPi * p + phase)
                        val driftY = anchor.radiusPx * 0.25f * cos(twoPi * p + phase * 1.3f)
                        val alpha = anchor.alpha * scene.midgroundAlpha * boost
                        if (alpha > 0.005f) {
                            drawCircle(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                anchor.color.copy(alpha = alpha),
                                                anchor.color.copy(alpha = alpha * 0.35f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(anchor.x + driftX, anchor.y + driftY),
                                        radius = anchor.radiusPx,
                                    ),
                                radius = anchor.radiusPx,
                                center = Offset(anchor.x + driftX, anchor.y + driftY),
                            )
                        }
                    }
                }
            },
    )
}

/** 单颗光斑的确定性锚点：位置 / 半径 / 亮度 / 取色均由 (seed, index) 推导。 */
private data class BokehAnchor(
    val x: Float,
    val y: Float,
    val radiusPx: Float,
    val alpha: Float,
    val color: Color,
)

private fun bokehAnchor(
    seed: Long,
    index: Int,
    width: Float,
    height: Float,
    palette: List<Color>,
): BokehAnchor {
    val rng = kotlin.random.Random(seed = seed * 1_000_003L + index + 707L)
    val minDimension = minOf(width, height)
    // 中央避让：拒绝式重采样（确定性），直到锚点落在边缘环带内。
    var x = 0f
    var y = 0f
    do {
        x = rng.nextFloat()
        y = rng.nextFloat()
    } while (abs(x - 0.5f) < CENTER_CLEAR_X && abs(y - 0.5f) < CENTER_CLEAR_Y)
    val radius = minDimension * (0.045f + rng.nextFloat() * 0.065f)
    val alpha = 0.05f + rng.nextFloat() * 0.06f
    return BokehAnchor(
        x = x * width,
        y = y * height,
        radiusPx = radius,
        alpha = alpha,
        color = palette[index % palette.size],
    )
}

/** 兜底光斑色：主题粒子色族为空时使用（正常主题均提供非空色族）。 */
private val FALLBACK_BOKEH_COLOR = Color(0xFF10A87C)
