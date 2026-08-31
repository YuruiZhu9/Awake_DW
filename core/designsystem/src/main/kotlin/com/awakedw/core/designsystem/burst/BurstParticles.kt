package com.awakedw.core.designsystem.burst

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.particles.ParticleMath
import kotlin.random.Random

/** 单次迸发粒子数：契约 8–12，取中庸的 10。 */
private const val BURST_COUNT = 10

/** 迸发飞行总时长：900ms。 */
private const val BURST_DURATION_NANOS = 900_000_000L

/** 距离锚点：飞行距离 = 锚点 × 每粒随机比例带。 */
private val DISTANCE_ANCHOR = 56.dp

/** 每粒飞行距离相对锚点的随机带（≥0.75 保证明显离手）。 */
private const val DISTANCE_MIN_FRACTION = 0.75f
private const val DISTANCE_MAX_FRACTION = 1.15f

/** 均匀角分布上允许的最大角度抖动（弧度），保持环状分布不被打散。 */
private const val MAX_ANGLE_JITTER_RAD = 0.18f

/** 进度值越接近该阈值即视为完成态，停止绘制开销。 */
private const val IDLE_THRESHOLD = 0.9999f

/** 空闲完成态的路径进度值：==1f 时 drawBehind 完全跳过。 */
private const val TRAVEL_IDLE = 1f

/** 兜底迸发色。 */
private val FALLBACK_COLOR = Color(0xFF10A87C)

/** 辉光同心环：相对实心半径的放大倍数与低透明度系数。 */
private const val GLOW_RING_SCALE = 1.8f
private const val GLOW_RING_ALPHA = 0.22f

/**
 * 触发式庆祝迸发：[trigger] 每自增一次，就从 [origin]（本组件坐标空间内）
 * 向四周迸出 [BURST_COUNT] 颗彩点——900ms ease-out 沿各自角度飞散、透明度由 1 收至 0。
 *
 * 行为契约：
 * - [trigger] <= 0 视为非触发态：立即清场（travelState 复位 idle）且不回调 [onFinish]；
 * - [onFinish] 仅在本次动画自然结束时调用一次；进行中被新一次 [trigger]
 *   打断则旧的静默取消，不误发回调；
 * - 每次迸发的角度抖动 / 距离 / 半径由 `Random(trigger)` 确定性生成，
 *   同一 trigger 序号重放结果一致；
 * - 运动学来自 [ParticleMath.burst] 纯函数；发光体用同心圆双层绘制，无 BlurFilter。
 *
 * 组件自身铺满父容器 [fillMaxSize]，请叠加在其他内容之上使用。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun BurstParticles(
    origin: Offset,
    colors: List<Color>,
    trigger: Int,
    onFinish: () -> Unit,
) {
    // travel 处于空闲完成态即无绘制；初始 idle 保证 trigger<=0 时静默。
    val travelState = remember { mutableFloatStateOf(TRAVEL_IDLE) }
    val travel by travelState

    val anchorPx = with(LocalDensity.current) { DISTANCE_ANCHOR.toPx() }
    val plan =
        remember(trigger) {
            buildBurstPlan(trigger, colors, anchorPx)
        }

    LaunchedEffect(trigger) {
        // 守卫（终审 T8a）：非正触发立即清场复位 idle，且不误发 onFinish——
        // 防止上次迸发进行中被重置为 0 后残影冻结在半程。
        if (trigger <= 0) {
            travelState.floatValue = TRAVEL_IDLE
            return@LaunchedEffect
        }
        travelState.floatValue = 0f
        var last = withFrameNanos { it }
        var elapsed = 0L
        while (elapsed < BURST_DURATION_NANOS) {
            val now = withFrameNanos { it }
            elapsed += now - last
            last = now
            val linear = minOf(elapsed / BURST_DURATION_NANOS.toFloat(), 1f)
            // 缓动作用于「路径进度」本身；ParticleMath.burst 保持线性纯函数。
            travelState.floatValue = EaseOutCubic.transform(linear)
        }
        travelState.floatValue = TRAVEL_IDLE
        onFinish()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (travel >= IDLE_THRESHOLD || travel < 0f) return@drawBehind
                    for (spec in plan) {
                        val frame =
                            ParticleMath.burst(
                                travel01 = travel,
                                origin = origin,
                                angleRad = spec.angleRad,
                                distancePx = spec.distancePx,
                            )
                        if (frame.glow) {
                            drawCircle(
                                color = spec.color.copy(alpha = frame.alpha * GLOW_RING_ALPHA),
                                radius = frame.radiusPx * GLOW_RING_SCALE,
                                center = frame.center,
                            )
                        }
                        drawCircle(spec.color.copy(alpha = frame.alpha), radius = frame.radiusPx, center = frame.center)
                    }
                },
    )
}

/** 单颗迸发粒子的确定性参数。 */
private data class BurstSpec(
    val angleRad: Float,
    val distancePx: Float,
    val color: Color,
)

/**
 * 由 `(trigger, colors, anchorPx)` 确定性构建一届迸发的参数表：
 * 角度均分全周加轻微抖动，距离与配色按序循环取用。
 */
private fun buildBurstPlan(
    trigger: Int,
    colors: List<Color>,
    anchorPx: Float,
): List<BurstSpec> {
    val rng = Random(trigger * 100_003L + 7L)
    val safeColors = if (colors.isEmpty()) listOf(FALLBACK_COLOR) else colors
    val stepRad = (2.0 * kotlin.math.PI / BURST_COUNT).toFloat()
    return List(BURST_COUNT) { index ->
        BurstSpec(
            angleRad = index * stepRad + rng.nextFloat() * 2f * MAX_ANGLE_JITTER_RAD - MAX_ANGLE_JITTER_RAD,
            distancePx = anchorPx * (DISTANCE_MIN_FRACTION + rng.nextFloat() * (DISTANCE_MAX_FRACTION - DISTANCE_MIN_FRACTION)),
            color = safeColors[index % safeColors.size],
        )
    }
}
