package com.awakedw.core.designsystem.ring

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 弧线宽度占环最短边长的比例。 */
const val RING_STROKE_FRACTION = 0.055f

/** 进度弧起点：正上方（12 点方向）。 */
private const val START_ANGLE_DEGREES = -90f

/** 按压缩放比例：轻按即有回弹，温柔不打扰。 */
private const val PRESS_SCALE = 0.97f

/** 认领阈值：切向扫过超过此角度（度）才把手势认领为「拖环」，纵向滚动优先（2.2.0）。 */
private const val CLAIM_THRESHOLD_DEGREES = 8f

/** 环带内界（占半径比例）：环心数值区不参与拖环认领。 */
private const val RING_BAND_INNER_FRACTION = 0.45f

/** 环带外界（占半径比例）：盒角（圆外）不参与拖环认领。 */
private const val RING_BAND_OUTER_FRACTION = 1.15f

/** 拖环预览弧透明度：主色 35%，只做方向感，不与值弧抢戏。 */
private const val PREVIEW_ARC_ALPHA = 0.35f

/** 松手后预览弧收场时长。 */
private const val PREVIEW_COLLAPSE_MS = 150

/**
 * 治愈系进度环（设计规格首页核心件）。
 *
 * - 轨道为整圆，取 [com.awakedw.core.designsystem.ThemeSpec.ringTrack]；
 *   值弧走主色 primary；圆头笔触、自顶部顺时针铺开；
 * - [progress] 变化经一阶弹簧（阻尼 0.9、中低刚度）温柔追赶——读数瞬间变化而弧线「到位有 settle」；
 * - [onRingTap] 非空时整环区域可点，按压时整体缩至 0.97 并以 spring 回弹；
 * - [onRingDrag]/[onRingDragEnd] 非空时环即把手（2.2.0）：切向拖动被认领为「拖环」手势，
 *   逐帧回传自按下点的扫过角（顺时针为正，度），松手回传总扫过角。认领前纵向滚动优先——
 *   父级一旦消费位移本手势静默退出；认领后消费位移、点按自动取消，并伴随一次认领轻震。
 *   本组件只感知几何：角度换算毫升与成笔都归调用方；
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
    onRingDrag: ((Float) -> Unit)? = null,
    onRingDragEnd: ((Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    // 预览弧的当前角度：只在 draw 相读取，拖动全程不触发重组；收场动画在独立协程里写回。
    val previewSweep = remember { mutableFloatStateOf(0f) }
    val collapseJob = remember { arrayOf<Job?>(null) }
    val animatedProgress =
        if (reduceMotion) {
            progress.coerceIn(0f, 1f)
        } else {
            animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
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

    val tapModifier =
        if (onRingTap != null) {
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

    val dragModifier =
        if (onRingDrag != null && onRingDragEnd != null) {
            Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = minOf(size.width, size.height) / 2f
                    var claimed = false
                    var swept = 0f
                    var lastAngle = pointerAngle(down.position, center)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUp()) break
                        // 让位：滚动等父级先消费了位移——本次手势静默退出，不与滚动争抢。
                        if (!claimed && event.changes.any { it.isConsumed }) break
                        val angle = pointerAngle(change.position, center)
                        swept += shortestAngleDelta(lastAngle, angle)
                        lastAngle = angle
                        // 认领三要件：累计扫过超阈值、指针在环带内、本次位移切向主导——
                        // 环心起手的滚动、盒角的竖滑、沿半径的拉拽都因此让位给滚动。
                        if (!claimed && abs(swept) > CLAIM_THRESHOLD_DEGREES && isTangentialClaim(change, center, radius)) {
                            claimed = true
                            collapseJob[0]?.cancel()
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        if (claimed) {
                            change.consume()
                            onRingDrag(swept)
                            previewSweep.floatValue = minOf(abs(swept), 360f)
                        }
                    }
                    if (claimed) {
                        onRingDragEnd(swept)
                        // 预览弧收场：直接操纵的余韵；减少动态时立即归零。
                        if (reduceMotion) {
                            previewSweep.floatValue = 0f
                        } else {
                            collapseJob[0] =
                                scope.launch {
                                    val start = previewSweep.floatValue
                                    val durationNanos = PREVIEW_COLLAPSE_MS * 1_000_000L
                                    var last = withFrameNanos { it }
                                    var accumulated = 0L
                                    while (accumulated < durationNanos) {
                                        val now = withFrameNanos { it }
                                        accumulated += now - last
                                        last = now
                                        val t = (accumulated.toFloat() / durationNanos).coerceIn(0f, 1f)
                                        previewSweep.floatValue = start * (1f - t)
                                    }
                                    previewSweep.floatValue = 0f
                                }
                        }
                    }
                }
            }
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .then(dragModifier)
                .then(tapModifier)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .drawBehind {
                    val minSide = minOf(size.width, size.height)
                    val stroke = RING_STROKE_FRACTION * minSide
                    val arcBounds = Size(minSide - stroke, minSide - stroke)
                    val topLeft =
                        Offset(
                            x = center.x - arcBounds.width / 2f,
                            y = center.y - arcBounds.height / 2f,
                        )
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
                    if (previewSweep.floatValue > 0.5f) {
                        // 拖环预览（2.2.0）：自当前弧端顺时针按扫过角铺一段主色 35% 弧——
                        // 只进 draw 相（内部 Animatable 驱动），拖动全程不触发重组。
                        drawArc(
                            color = spec.primary.copy(alpha = PREVIEW_ARC_ALPHA),
                            startAngle = START_ANGLE_DEGREES + animatedProgress * 360f,
                            sweepAngle = previewSweep.value,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcBounds,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
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
                },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 指针相对环心的角度（度；y 轴向下的屏幕坐标系里顺时针为增，与值弧铺向一致）。 */
private fun pointerAngle(
    position: Offset,
    center: Offset,
): Float = Math.toDegrees(atan2((position.y - center.y).toDouble(), (position.x - center.x).toDouble())).toFloat()

/** 两次角度读数的最短差（(-180, 180]），跨 ±180° 缠绕不断档。 */
private fun shortestAngleDelta(
    from: Float,
    to: Float,
): Float = ((to - from + 540f) % 360f) - 180f

/**
 * 认领几何：指针在环带内（[RING_BAND_INNER_FRACTION]–[RING_BAND_OUTER_FRACTION] 倍半径），
 * 且本次位移切向主导（位移与半径的叉积大于点积——沿环走而非沿径走）。
 */
private fun isTangentialClaim(
    change: PointerInputChange,
    center: Offset,
    radius: Float,
): Boolean {
    val dx = change.position.x - center.x
    val dy = change.position.y - center.y
    val r = sqrt(dx * dx + dy * dy)
    if (r < RING_BAND_INNER_FRACTION * radius || r > RING_BAND_OUTER_FRACTION * radius) return false
    val moveX = change.position.x - change.previousPosition.x
    val moveY = change.position.y - change.previousPosition.y
    val cross = abs(dx * moveY - dy * moveX)
    val dot = abs(dx * moveX + dy * moveY)
    return cross > dot
}
