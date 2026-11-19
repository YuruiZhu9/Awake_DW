package com.awakedw.core.designsystem.ring

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.animation.pressScale
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.artworkSourceRect
import com.awakedw.core.designsystem.paper.paperColorOf
import com.awakedw.core.designsystem.rememberReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** 弧线宽度占环最短边长的比例。 */
const val RING_STROKE_FRACTION = 0.055f

/** 进度弧起点：正上方（12 点方向）。 */
private const val START_ANGLE_DEGREES = -90f

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

/** 拖环提示：空态首次出现前的静置、单次往返时长、最大扫过角与透明度。 */
private const val DRAG_HINT_DELAY_MS = 600L
private const val DRAG_HINT_DURATION_MS = 1_100L
private const val DRAG_HINT_SWEEP_DEGREES = 26f
private const val DRAG_HINT_ALPHA = 0.52f

/** 窗环（Phase 1）：中心纸面圆牌半径与 60 格刻度的内外半径，都相对弧半径。 */
private const val LENS_MEDALLION_FRACTION = 0.70f
private const val TICK_OUTER_FRACTION = 0.88f
private const val TICK_INNER_MAJOR_FRACTION = 0.74f
private const val TICK_INNER_MINOR_FRACTION = 0.78f
private const val TICK_COUNT = 60
private const val TICK_MAJOR_EVERY = 5

/** 窗景角花基础不透明度：压到 0.92，让白蕾丝不刺眼、也仍像一张照片压在纸下。 */
private const val LENS_ART_ALPHA = 0.92f

/**
 * 拖环提示的扫过角：正弦包络，0 起、峰值 [DRAG_HINT_SWEEP_DEGREES]、1 回零。
 * 只进 draw 相，不改变真实进度；纯函数便于钉住提示的运动学。
 */
internal fun dragHintSweepAt(progress: Float): Float = (sin(Math.PI * progress.coerceIn(0f, 1f)) * DRAG_HINT_SWEEP_DEGREES).toFloat()

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
 * - [showDragHint] 为 true 时（空态首页）在环端播放一次 26° 的「可拖动」提示：虚线弧 + 珠点
 *   前移回位；只进 draw 相、减少动态不播放；任何点按/拖环/记录后由调用方关闭；
 * - [lens] 非空时，环内垫一张放大的主题角花 + 60 格刻度 + 中心纸面圆牌（大改版 Phase 1 窗环）。
 *   窗景只改变绘制，不参与手势；默认 null 时外观与旧环完全一致；
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
    showDragHint: Boolean = false,
    lens: RingLens? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    // 预览弧的当前角度：只在 draw 相读取，拖动全程不触发重组；收场动画在独立协程里写回。
    val previewSweep = remember { mutableFloatStateOf(0f) }
    // 空态拖环提示：同样只进 draw 相，不改变真实进度、不触发重组。
    val hintProgress = remember { mutableFloatStateOf(0f) }
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
    // onRingDrag 是方法引用、每次重组可能换实例，不能直接做 key；只认「是否具备拖动能力」。
    val hintEnabled = showDragHint && onRingDrag != null
    // 一次进程内只演一次：标签切换回首页不重复播放；静态文案仍会教学。
    var hintPlayed by rememberSaveable { mutableStateOf(false) }

    // 空态的一次性「可拖动」提示：只动提示珠，不动真实进度；任何交互由调用方关闭。
    LaunchedEffect(hintEnabled, reduceMotion) {
        if (!hintEnabled || reduceMotion || hintPlayed) {
            hintProgress.floatValue = 0f
            return@LaunchedEffect
        }
        hintPlayed = true
        delay(DRAG_HINT_DELAY_MS)
        val durationNanos = DRAG_HINT_DURATION_MS * 1_000_000L
        var last = withFrameNanos { it }
        var accumulated = 0L
        while (accumulated < durationNanos) {
            val now = withFrameNanos { it }
            accumulated += now - last
            last = now
            hintProgress.floatValue = (accumulated.toFloat() / durationNanos).coerceIn(0f, 1f)
        }
        hintProgress.floatValue = 0f
    }

    val tapModifier =
        if (onRingTap != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                // 环心文案只陈述进度；读屏点按动作要补一句「这一下会发生什么」。
                onClickLabel = "记一杯",
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
                .pressScale(interactionSource)
                .drawWithCache {
                    // 几何与渐变在缓存段一次构建（2.6.0 绘制审计）：弹簧逐帧只重铺弧，不重建笔刷。
                    // 缓存段 receiver 是 CacheDrawScope：center 需按 size 自算（DrawScope 成员不可见）。
                    val ringCenter = Offset(size.width / 2f, size.height / 2f)
                    val minSide = minOf(size.width, size.height)
                    val stroke = RING_STROKE_FRACTION * minSide
                    val arcBounds = Size(minSide - stroke, minSide - stroke)
                    val arcStroke = Stroke(width = stroke, cap = StrokeCap.Round)
                    val paper = paperColorOf(spec)
                    val pearlHaloStroke = Stroke(width = 1.dp.toPx())
                    val hintStroke =
                        Stroke(
                            width = stroke * 0.68f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 0.70f, stroke * 0.75f)),
                        )
                    val topLeft =
                        Offset(
                            x = ringCenter.x - arcBounds.width / 2f,
                            y = ringCenter.y - arcBounds.height / 2f,
                        )
                    val valueArcBrush =
                        Brush.sweepGradient(
                            listOf(
                                spec.primary.copy(alpha = 0.78f),
                                spec.primary,
                                spec.primary.copy(alpha = 0.90f),
                            ),
                        )
                    // 窗环几何（Phase 1）：角花铺到值弧半径，圆牌与刻度都按弧半径取比例。
                    val arcRadius = arcBounds.width / 2f
                    val lensSrcRect = lens?.let { artworkSourceRect(it.image.width, it.image.height, it.crop) }
                    val lensDstRect =
                        Rect(
                            left = ringCenter.x - arcRadius,
                            top = ringCenter.y - arcRadius,
                            right = ringCenter.x + arcRadius,
                            bottom = ringCenter.y + arcRadius,
                        )
                    val lensClipPath = Path().apply { addOval(lensDstRect) }
                    val lensVeilAlpha = lens?.veilAlpha ?: 0f
                    val lensVeilBrush =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0f to Color.Transparent,
                                    0.55f to paper.copy(alpha = lensVeilAlpha * 0.40f),
                                    1f to paper.copy(alpha = lensVeilAlpha),
                                ),
                            center = ringCenter,
                            radius = arcRadius,
                        )
                    val medallionRadius = arcRadius * LENS_MEDALLION_FRACTION
                    val tickOuterRadius = arcRadius * TICK_OUTER_FRACTION
                    val tickMajorInnerRadius = arcRadius * TICK_INNER_MAJOR_FRACTION
                    val tickMinorInnerRadius = arcRadius * TICK_INNER_MINOR_FRACTION
                    onDrawBehind {
                        if (lens != null && lensSrcRect != null) {
                            // 环内窗景：角花垫底，遮罩/圆牌/刻度依次压上；手势仍归外层。
                            clipPath(lensClipPath) {
                                drawImage(
                                    image = lens.image,
                                    srcOffset = IntOffset(lensSrcRect.left.roundToInt(), lensSrcRect.top.roundToInt()),
                                    srcSize = IntSize(lensSrcRect.width.roundToInt(), lensSrcRect.height.roundToInt()),
                                    dstOffset = IntOffset(lensDstRect.left.roundToInt(), lensDstRect.top.roundToInt()),
                                    dstSize = IntSize(lensDstRect.width.roundToInt(), lensDstRect.height.roundToInt()),
                                    alpha = LENS_ART_ALPHA,
                                )
                                drawCircle(brush = lensVeilBrush, radius = arcRadius, center = center)
                            }
                            drawCircle(
                                color = Color.White.copy(alpha = 0.20f),
                                radius = arcRadius - 0.5.dp.toPx(),
                                center = center,
                                style = Stroke(width = 1.dp.toPx()),
                            )
                            if (lens.medallionAlpha > 0.01f) {
                                drawCircle(
                                    color = paper.copy(alpha = lens.medallionAlpha),
                                    radius = medallionRadius,
                                    center = center,
                                )
                                drawCircle(
                                    color = spec.laceColor.copy(alpha = 0.32f * lens.medallionAlpha),
                                    radius = medallionRadius,
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx()),
                                )
                            }
                            for (index in 0 until TICK_COUNT) {
                                val angle = index / TICK_COUNT.toFloat() * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f
                                val major = index % TICK_MAJOR_EVERY == 0
                                val inner = if (major) tickMajorInnerRadius else tickMinorInnerRadius
                                drawLine(
                                    color =
                                        if (major) {
                                            spec.ringValueText.copy(alpha = 0.55f)
                                        } else {
                                            spec.greetingSubColor.copy(alpha = 0.30f)
                                        },
                                    start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                                    end = Offset(center.x + cos(angle) * tickOuterRadius, center.y + sin(angle) * tickOuterRadius),
                                    strokeWidth = if (major) 1.1.dp.toPx() else 0.6.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        }
                        drawCircle(
                            color = spec.ringTrack,
                            radius = arcBounds.width / 2f,
                            center = center,
                            style = arcStroke,
                        )
                        drawArc(
                            brush = valueArcBrush,
                            startAngle = START_ANGLE_DEGREES,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcBounds,
                            style = arcStroke,
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
                                style = arcStroke,
                            )
                        }
                        val hintT = hintProgress.floatValue
                        if (hintT > 0f && hintT < 1f) {
                            // 空态提示（2.10.0）：虚线弧 + 珠点自弧端前移 26° 再回位，演示「环可以拖」。
                            val hintSweep = dragHintSweepAt(hintT)
                            val envelope = (sin(Math.PI * hintT)).toFloat().coerceIn(0f, 1f)
                            if (hintSweep > 0.5f) {
                                val hintStart = START_ANGLE_DEGREES + animatedProgress * 360f
                                drawArc(
                                    color = spec.primary.copy(alpha = DRAG_HINT_ALPHA * envelope),
                                    startAngle = hintStart,
                                    sweepAngle = hintSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcBounds,
                                    style = hintStroke,
                                )
                                val hintEndAngle = Math.toRadians((hintStart + hintSweep).toDouble())
                                val hintEnd =
                                    Offset(
                                        x = center.x + arcBounds.width / 2f * cos(hintEndAngle).toFloat(),
                                        y = center.y + arcBounds.height / 2f * sin(hintEndAngle).toFloat(),
                                    )
                                drawCircle(
                                    color = spec.primary.copy(alpha = 0.82f * envelope),
                                    radius = stroke * 0.34f,
                                    center = hintEnd,
                                )
                            }
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
                                style = pearlHaloStroke,
                            )
                            drawCircle(
                                color = spec.laceColor.copy(alpha = 0.90f),
                                radius = stroke * 0.30f,
                                center = end,
                            )
                        }
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
