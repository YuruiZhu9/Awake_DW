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
import com.awakedw.core.designsystem.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 半径锚点：各层半径因子（ParticleMath 半径带）× 该值的像素换算。 */
private val ANCHOR_SIZE = 40.dp

/** 整个上浮循环的时长（48s）：progress01 走满一圈，粒子恰好无缝回卷一次。 */
private const val LOOP_NANOS = 48_000_000_000L

/** 「✦」星芒字形。 */
private const val STAR_GLYPH = "✦"

// 两枚「✦」星芒的固定相位锚点（屏宽比 to 屏高比）与呼吸参数；刻意不含随机数。
private val STAR_ANCHORS = listOf(0.24f to 0.28f, 0.76f to 0.60f, 0.38f to 0.74f)
private val STAR_FONT_SIZES = listOf(16.sp, 12.sp, 14.sp)
private val STAR_TURNS = listOf(2, 3, 2) // 每循环内呼吸次数（整数 ⇒ 循环闭合）
private val STAR_PHASES = listOf(0f, PI.toFloat() * 2f / 3f, PI.toFloat() * 4f / 3f)
private const val STAR_ALPHA_BASE = 0.20f
private const val STAR_ALPHA_AMPLITUDE = 0.45f

// 两朵「甜系小花」（§12 L2）：固定锚点五瓣花，整循环慢转一圈（整数转 ⇒ 闭合）。
private val FLOWER_ANCHORS = listOf(0.14f to 0.52f, 0.86f to 0.80f)
private const val FLOWER_ALPHA_BASE = 0.14f
private const val FLOWER_ALPHA_AMPLITUDE = 0.10f
private const val FLOWER_TURNS = 1

/** 页面职责对应的粒子密度：首页/设置/引导安静，统计/开屏保留标准层次。 */
enum class ParticleDensity(
    internal val dotCount: Int,
    internal val accentAlphaScale: Float,
) {
    QUIET(dotCount = 14, accentAlphaScale = 0.72f),
    STANDARD(dotCount = ParticleMath.DOT_COUNT, accentAlphaScale = 1f),
}

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
    showStars: Boolean = true,
    showFlowers: Boolean = true,
    density: ParticleDensity = ParticleDensity.STANDARD,
) {
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
                progress.floatValue = (accumulated % LOOP_NANOS).toFloat() / LOOP_NANOS
            }
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
                val starLayouts = if (showStars) starStyles.map { style -> textMeasurer.measure(STAR_GLYPH, style) } else emptyList()
                onDrawBehind {
                    val p = progress.floatValue
                    for (index in 0 until density.dotCount) {
                        val frame = ParticleMath.floating(index, seed, anchorPx, p, area)
                        val color = colorAt(colors, index)
                        if (frame.glow) {
                            drawCircle(
                                color = color.copy(alpha = frame.alpha * GLOW_RING_ALPHA * density.accentAlphaScale),
                                radius = frame.radiusPx * GLOW_RING_SCALE,
                                center = frame.center,
                            )
                        }
                        drawCircle(
                            color = color.copy(alpha = frame.alpha * density.accentAlphaScale),
                            radius = frame.radiusPx,
                            center = frame.center,
                        )
                        // 珍珠高光（§12）：左上一点白，圆点即成光珠。
                        drawCircle(
                            color = Color.White.copy(alpha = frame.alpha * 0.6f * density.accentAlphaScale),
                            radius = frame.radiusPx * 0.28f,
                            center = frame.center - Offset(frame.radiusPx * 0.32f, frame.radiusPx * 0.32f),
                        )
                    }
                    if (showStars) {
                        for ((index, layout) in starLayouts.withIndex()) {
                            val twinkle =
                                0.5f + 0.5f *
                                    sin(2f * PI.toFloat() * STAR_TURNS[index] * p + STAR_PHASES[index])
                            val bob = sin(2f * PI.toFloat() * STAR_TURNS[index] * p + STAR_PHASES[index]) * anchorPx * BOB_AMPLITUDE
                            val centerX = STAR_ANCHORS[index].first * area.width
                            val centerY = STAR_ANCHORS[index].second * area.height + bob
                            drawText(
                                textLayoutResult = layout,
                                color = colorAt(colors, density.dotCount + index),
                                alpha = (STAR_ALPHA_BASE + STAR_ALPHA_AMPLITUDE * twinkle) * density.accentAlphaScale,
                                topLeft =
                                    Offset(
                                        centerX - layout.size.width / 2f,
                                        centerY - layout.size.height / 2f,
                                    ),
                            )
                        }
                    }
                    if (showFlowers) {
                        FLOWER_ANCHORS.forEachIndexed { fi, anchor ->
                            val flowerAlpha =
                                FLOWER_ALPHA_BASE +
                                    FLOWER_ALPHA_AMPLITUDE * (0.5f + 0.5f * sin(2f * PI.toFloat() * p + fi * PI.toFloat() * 2f / 3f))
                            val center = Offset(anchor.first * area.width, anchor.second * area.height)
                            drawFlower(
                                center = center,
                                orbit = anchorPx * 0.5f,
                                petalRadius = anchorPx * 0.2f,
                                rotation = p,
                                color = colorAt(colors, density.dotCount + ParticleMath.STAR_COUNT + fi),
                                alpha = flowerAlpha * density.accentAlphaScale,
                            )
                        }
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

/**
 * 五瓣小花（§12 L2）：[orbit] 花心到瓣心的轨道半径，整循环慢转 [FLOWER_TURNS] 圈。
 * 只在 onDrawBehind 内调用（DrawScope 扩展，不进语义树）。
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlower(
    center: Offset,
    orbit: Float,
    petalRadius: Float,
    rotation: Float,
    color: Color,
    alpha: Float,
) {
    val baseAngle = 2f * PI.toFloat() * FLOWER_TURNS * rotation
    repeat(5) { petal ->
        val angle = baseAngle + petal * (2f * PI.toFloat() / 5f)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = petalRadius,
            center = center + Offset(orbit * cos(angle), orbit * sin(angle)),
        )
    }
    drawCircle(color = Color.White.copy(alpha = alpha * 0.8f), radius = petalRadius * 0.55f, center = center)
}
