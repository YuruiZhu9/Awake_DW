package com.awakedw.core.designsystem.particles

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/** 半径锚点：各层半径因子（ParticleMath 半径带）× 该值的像素换算。 */
private val ANCHOR_SIZE = 40.dp

/** 整个上浮循环的时长（48s）：progress01 走满一圈，粒子恰好无缝回卷一次。 */
private const val LOOP_NANOS = 48_000_000_000L

/** 「✦」星芒字形。 */
private const val STAR_GLYPH = "✦"

// 三枚「✦」星芒的固定相位锚点（屏宽比 to 屏高比）与呼吸参数；刻意不含随机数。
private val STAR_ANCHORS = listOf(0.24f to 0.28f, 0.76f to 0.60f, 0.38f to 0.74f)
private val STAR_FONT_SIZES = listOf(16.sp, 12.sp, 14.sp)
private val STAR_TURNS = listOf(2, 3, 2) // 每循环内呼吸次数（整数 ⇒ 循环闭合）
private val STAR_PHASES = listOf(0f, PI.toFloat() * 2f / 3f, PI.toFloat() * 4f / 3f)
private const val STAR_ALPHA_BASE = 0.20f
private const val STAR_ALPHA_AMPLITUDE = 0.45f

/** 兜底粒子色：colors 为空时避免取越界（正常主题均提供非空 particleColors）。 */
private val FALLBACK_COLOR = Color(0xFF10A87C)

/**
 * 分层漂浮粒子场（规格 §2.2）：大 2 / 中 8 / 小 14 共 [ParticleMath.DOT_COUNT]
 * 颗三层圆点 + 3 枚固定相位「✦」星芒，整体极缓上浮、顶部渐隐、底部无缝回卷。
 *
 * 帧驱动契约：
 * - 运动学全部来自 [ParticleMath.floating] 纯函数，`(index, seed)` 确定性推导，
 *   组合内不持有任何随机对象——重组永不重采样；
 * - 进度存于 draw 阶段读取的 `mutableFloatStateOf`，逐帧 `withFrameNanos` 推进，
 *   仅触发重绘、不触发本组合体重组；
 * - 辉光 = 实心圆 + 放大低透明度同心圆双层绘制，规避 BlurFilter 的逐帧全屏模糊开销。
 *
 * @param colors 粒子色族（通常传 [com.awakedw.core.designsystem.ThemeSpec.particleColors]）
 * @param seed   随机种子，默认 7；同一布局内铺两块粒子场时可错开
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun FloatingParticles(
    colors: List<Color>,
    modifier: Modifier,
    seed: Long = 7L,
) {
    val progress = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(seed) {
        var last = withFrameNanos { it }
        var accumulated = 0L
        while (true) {
            val now = withFrameNanos { it }
            accumulated += now - last
            last = now
            progress.floatValue = (accumulated % LOOP_NANOS).toFloat() / LOOP_NANOS
        }
    }

    val anchorPx = with(LocalDensity.current) { ANCHOR_SIZE.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val starStyles = STAR_FONT_SIZES.map { TextStyle(fontSize = it) }

    Box(
        modifier =
            modifier.drawWithCache {
                val area = size
                // 文字排版结果仅在尺寸/样式变化时重排一次，帧间只变 alpha。
                val starLayouts = starStyles.map { style -> textMeasurer.measure(STAR_GLYPH, style) }
                onDrawBehind {
                    val p = progress.floatValue
                    for (index in 0 until ParticleMath.DOT_COUNT) {
                        val frame = ParticleMath.floating(index, seed, anchorPx, p, area)
                        val color = colorAt(colors, index)
                        if (frame.glow) {
                            drawCircle(
                                color = color.copy(alpha = frame.alpha * GLOW_RING_ALPHA),
                                radius = frame.radiusPx * GLOW_RING_SCALE,
                                center = frame.center,
                            )
                        }
                        drawCircle(color.copy(alpha = frame.alpha), radius = frame.radiusPx, center = frame.center)
                    }
                    for ((index, layout) in starLayouts.withIndex()) {
                        val twinkle =
                            0.5f + 0.5f *
                                sin(2f * PI.toFloat() * STAR_TURNS[index] * p + STAR_PHASES[index])
                        val bob = sin(2f * PI.toFloat() * STAR_TURNS[index] * p + STAR_PHASES[index]) * anchorPx * BOB_AMPLITUDE
                        val centerX = STAR_ANCHORS[index].first * area.width
                        val centerY = STAR_ANCHORS[index].second * area.height + bob
                        drawText(
                            textLayoutResult = layout,
                            color = colorAt(colors, ParticleMath.DOT_COUNT + index),
                            alpha = STAR_ALPHA_BASE + STAR_ALPHA_AMPLITUDE * twinkle,
                            topLeft =
                                Offset(
                                    centerX - layout.size.width / 2f,
                                    centerY - layout.size.height / 2f,
                                ),
                        )
                    }
                }
            },
    )
}

/** 辉光同心环：相对实心半径的放大倍数与低透明度系数。 */
private const val GLOW_RING_SCALE = 2f
private const val GLOW_RING_ALPHA = 0.18f

/** 星芒随呼吸上下浮动幅度（相对半径锚点）。 */
private const val BOB_AMPLITUDE = 0.10f

/** 按序取色并循环使用；空列表走兜底色，负下标亦可安全取模。 */
private fun colorAt(
    colors: List<Color>,
    index: Int,
): Color = if (colors.isEmpty()) FALLBACK_COLOR else colors[((index % colors.size) + colors.size) % colors.size]
