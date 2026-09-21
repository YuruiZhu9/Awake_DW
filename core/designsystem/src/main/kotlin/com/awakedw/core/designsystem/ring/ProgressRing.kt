package com.awakedw.core.designsystem.ring

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 弧线宽度占环最短边长的比例。 */
const val RING_STROKE_FRACTION = 0.055f

/** 进度弧起点：正上方（12 点方向）。 */
private const val START_ANGLE_DEGREES = -90f

/** 按压缩放比例：轻按即有回弹，温柔不打扰。 */
private const val PRESS_SCALE = 0.97f

/** 进度变化跟随缓动时长（ms）。 */
private const val PROGRESS_TWEEN_MS = 600

// —— 环体液化（1.8.0）——

/** 液面最高填充（占内圆直径比）：满杯也在杯沿留一圈空气，波面始终可见。 */
private const val LIQUID_MAX_FILL = 0.94f

/** 液体主体透明度：氛围层，不与环心数字争读。 */
private const val LIQUID_BODY_ALPHA = 0.12f

/** 回声波层透明度：半拍慢波只给深度，不给注意力。 */
private const val LIQUID_ECHO_ALPHA = 0.06f

/** 液面高光线透明度。 */
private const val LIQUID_SURFACE_ALPHA = 0.32f

/** 波面振幅（占内圆半径比）：缓而不惊。 */
private const val LIQUID_WAVE_AMPLITUDE = 0.022f

/** 可视直径内的波数：一段缓波，不碎。 */
private const val LIQUID_WAVE_COUNT = 1.25f

/**
 * 液面基准线 y（px）：进度 0 贴内圆底缘，满杯升到 [LIQUID_MAX_FILL] 对应的高度——
 * 杯里永远留一圈空气。进度越界按 0..1 钳制。
 */
internal fun liquidSurfaceY(
    progress: Float,
    centerY: Float,
    innerRadius: Float,
): Float {
    val fill = progress.coerceIn(0f, 1f) * LIQUID_MAX_FILL
    return centerY + innerRadius - 2f * innerRadius * fill
}

/** 波面在横向分数 [xFraction]（0..1）处相对基准面的偏移（px，正为向下）；相位以整周回绕，无缝循环。 */
internal fun waveSurfaceOffset(
    xFraction: Float,
    phase: Float,
    amplitudePx: Float,
): Float = (amplitudePx * sin(2.0 * PI * (xFraction * LIQUID_WAVE_COUNT + phase))).toFloat()

/**
 * 治愈系进度环（设计规格首页核心件）。
 *
 * - 轨道为整圆，取 [com.awakedw.core.designsystem.ThemeSpec.ringTrack]；
 *   值弧走主色 primary；圆头笔触、自顶部顺时针铺开；
 * - 环体液化（1.8.0）：进度 > 0 时内圈注水——液位随进度涨落，双层缓波 + 液面高光；
 *   减少动态下液面静止（仍显示水位），零进度不注水；
 * - [progress] 变化经 600ms FastOutSlowIn 缓动跟随——读数瞬间变化而弧线温柔追赶；
 * - [onRingTap] 非空时整环区域可点，按压时整体缩至 0.97 并以 spring 回弹；
 * - [content] 渲染在环心（通常为数值大字，取 ThemeSpec.ringValueText 色）。
 *
 * 尺寸由传入的 [modifier] 决定（建议保持正方形）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier,
    onRingTap: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    val animatedProgress =
        if (reduceMotion) {
            progress.coerceIn(0f, 1f)
        } else {
            animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = PROGRESS_TWEEN_MS, easing = FastOutSlowInEasing),
                label = "ringProgress",
            ).value
        }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale =
        if (reduceMotion) {
            1f
        } else {
            animateFloatAsState(
                targetValue = if (pressed) PRESS_SCALE else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "ringPressScale",
            ).value
        }

    // 液面波动相位（1.8.0）：只在有水且未减动态时运行；相位整周回绕无缝。
    // State 由 draw 层直读，波动逐帧只重绘不重组。
    val wavePhase: State<Float>? =
        if (reduceMotion || progress <= 0f) {
            null
        } else {
            rememberInfiniteTransition(label = "liquidWave")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = MotionTokens.LIQUID_WAVE_PERIOD_MS.toInt(), easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    label = "liquidWavePhase",
                )
        }

    val tapModifier =
        if (onRingTap != null) {
            val view = LocalView.current
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onRingTap()
            }
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .then(tapModifier)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .drawWithCache {
                    val minSide = minOf(size.width, size.height)
                    // CacheDrawScope 不带 DrawScope.center：缓存块自算圆心，onDrawBehind 一并复用。
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val stroke = RING_STROKE_FRACTION * minSide
                    val arcBounds = Size(minSide - stroke, minSide - stroke)
                    val topLeft =
                        Offset(
                            x = center.x - arcBounds.width / 2f,
                            y = center.y - arcBounds.height / 2f,
                        )
                    // 液体几何只随尺寸重建（P1 纪律：不逐帧分配 Path），逐帧只 rewrite 点位。
                    val innerRadius = arcBounds.width / 2f - stroke / 2f
                    val liquidClip =
                        Path().apply { addOval(Rect(center = Offset(center.x, center.y), radius = innerRadius)) }
                    val waveBody = Path()
                    val waveEcho = Path()
                    val waveSurface = Path()

                    fun traceWave(
                        path: Path,
                        phase: Float,
                        amplitudePx: Float,
                        surfaceY: Float,
                    ) {
                        val left = center.x - innerRadius
                        val right = center.x + innerRadius
                        path.rewind()
                        path.moveTo(left, surfaceY + waveSurfaceOffset(0f, phase, amplitudePx))
                        val steps = 36
                        for (i in 1..steps) {
                            val fraction = i / steps.toFloat()
                            path.lineTo(
                                left + (right - left) * fraction,
                                surfaceY + waveSurfaceOffset(fraction, phase, amplitudePx),
                            )
                        }
                        path.lineTo(right, center.y + innerRadius)
                        path.lineTo(left, center.y + innerRadius)
                        path.close()
                    }

                    onDrawBehind {
                        drawCircle(
                            color = spec.ringTrack,
                            radius = arcBounds.width / 2f,
                            center = center,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        drawArc(
                            brush =
                                Brush.sweepGradient(
                                    listOf(
                                        spec.primary.copy(alpha = 0.78f),
                                        spec.primary,
                                        spec.primary.copy(alpha = 0.90f),
                                    ),
                                ),
                            startAngle = START_ANGLE_DEGREES,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcBounds,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        if (animatedProgress > 0.01f) {
                            val endAngle = Math.toRadians((START_ANGLE_DEGREES + animatedProgress * 360f).toDouble())
                            val end =
                                Offset(
                                    x = center.x + arcBounds.width / 2f * cos(endAngle).toFloat(),
                                    y = center.y + arcBounds.height / 2f * sin(endAngle).toFloat(),
                                )
                            // 弧端珠扣（1.7.0）：与统计页细轨珍珠同一件饰语言——浅珠居弧端，外绕一圈发丝晕。
                            drawCircle(
                                color = spec.laceColor.copy(alpha = 0.40f),
                                radius = stroke * 0.52f,
                                center = end,
                                style = Stroke(width = 1.dp.toPx()),
                            )
                            drawCircle(
                                color = spec.laceColor.copy(alpha = 0.90f),
                                radius = stroke * 0.30f,
                                center = end,
                            )
                        }

                        // —— 环体液化（1.8.0）：杯内水位随进度涨落，双层缓波 + 液面高光 ——
                        if (animatedProgress > 0.005f) {
                            val surfaceY = liquidSurfaceY(animatedProgress, center.y, innerRadius)
                            val amplitude = innerRadius * LIQUID_WAVE_AMPLITUDE
                            val phase = wavePhase?.value ?: 0f
                            traceWave(waveEcho, phase + 0.5f, amplitude * 0.65f, surfaceY)
                            traceWave(waveBody, phase, amplitude, surfaceY)
                            clipPath(liquidClip) {
                                drawPath(waveEcho, color = spec.primary.copy(alpha = LIQUID_ECHO_ALPHA))
                                drawPath(waveBody, color = spec.primary.copy(alpha = LIQUID_BODY_ALPHA))
                            }
                            // 液面高光只描表面曲线（开路径），不画闭合的底与侧。
                            val left = center.x - innerRadius
                            waveSurface.rewind()
                            waveSurface.moveTo(left, surfaceY + waveSurfaceOffset(0f, phase, amplitude))
                            val steps = 36
                            for (i in 1..steps) {
                                val fraction = i / steps.toFloat()
                                waveSurface.lineTo(
                                    left + 2f * innerRadius * fraction,
                                    surfaceY + waveSurfaceOffset(fraction, phase, amplitude),
                                )
                            }
                            clipPath(liquidClip) {
                                drawPath(
                                    waveSurface,
                                    color = spec.primary.copy(alpha = LIQUID_SURFACE_ALPHA),
                                    style = Stroke(width = 1.2.dp.toPx()),
                                )
                            }
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
