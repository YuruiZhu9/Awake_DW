package com.awakedw.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.ring.ProgressRing
import com.awakedw.feature.home.HOME_RING_DIAMETER
import kotlinx.coroutines.delay

/** 水滴直径。 */
private val DROPLET_DIAMETER = 20.dp

/** 水滴起落高度差：y 自 -80dp 落至 0（规格 §2.3 spring 落点）。 */
private val DROPLET_FALL_DISTANCE = 80.dp

/** 涟漪起始半径（贴着水滴）。 */
private val RIPPLE_START_RADIUS = 14.dp

/** 形序段笔触占环半径比例（与 ProgressRing 的 0.085×最短边观感一致）。 */
private const val RING_STROKE_FRACTION = 0.085f

/** 首页进度环的初始观感（35%）：形序段涟漪外圈定格为该进度，与首页真实环观感衔接。 */
private const val INITIAL_RING_PROGRESS = 0.35f

/** 涟漪段笔触宽度。 */
private val RIPPLE_STROKE = 3.dp

/** 涟漪峰值不透明度：扩散过程由 1 收敛至 0。 */
private const val RIPPLE_MAX_ALPHA = 0.9f

/** 水滴落点纵向位置：与首页占位进度环的中心观感对齐。 */
private const val LANDING_Y_FRACTION = 0.42f

/** 形序 Crossfade 时长（ms），与 SplashSequencer 的 MORPH 段一致。 */
private const val MORPH_CROSSFADE_MS = 250

/** 自然放行后的交棒等待：Crossfade 已走完，只留半拍防尾帧截断。 */
private const val NATURAL_HANDOVER_MS = 40L

/** 点击跳过后的交棒等待：让「直达首页」的 Crossfade 走完，视觉无跳切。 */
private const val SKIPPED_HANDOVER_MS = 260L

/**
 * 开屏续场（规格 §2.3，Compose 内总 ~1.2s）：
 * 水滴自上而下 spring 落点（450ms，过冲回弹）→ 两圈涟漪扩散+透明度收敛
 * （各 380ms，相位差 120ms）→ 涟漪外圈放大为进度环初始态并 Crossfade(250ms)
 * 入首页种子预览 → 交棒导航壳（真首页）。任意点击经 [SplashSequencer.skip] 直达首页。
 *
 * 时序与绘制彻底分离：节奏由纯 JVM 的 [SplashSequencer] 决定，
 * 本组合只逐帧 tick + 渲染；冷启动仅播一次（配置变更不重放，见 AwakeNavHost）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun SplashMorph(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit,
) {
    val spec = currentThemeSpec()
    val sequencer = remember { SplashSequencer() }
    var frame by remember { mutableStateOf(sequencer.snapshot()) }

    // 帧驱动：逐帧推进状态机；DONE（自然/兜底/跳过）后按路径交棒。
    LaunchedEffect(sequencer) {
        var lastNanos = withFrameNanos { it }
        while (sequencer.phase != SplashPhase.DONE) {
            val nowNanos = withFrameNanos { it }
            sequencer.tick((nowNanos - lastNanos) / 1_000_000L)
            lastNanos = nowNanos
            frame = sequencer.snapshot()
        }
        frame = sequencer.snapshot()
        val handoverMs = if (sequencer.skipped) SKIPPED_HANDOVER_MS else NATURAL_HANDOVER_MS
        delay(handoverMs)
        onSplashFinished()
    }

    Crossfade(
        targetState = frame.phase == SplashPhase.MORPH || frame.phase == SplashPhase.DONE,
        animationSpec = tween(durationMillis = MORPH_CROSSFADE_MS),
        modifier =
            modifier
                .pointerInput(sequencer) {
                    detectTapGestures { sequencer.skip() }
                },
        label = "splashToHome",
    ) { showHome ->
        if (showHome) {
            HomeSeedPreview(modifier = Modifier.fillMaxSize())
        } else {
            SplashVisuals(frame = frame, spec = spec, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * 形序段终态的首页种子预览：真实首页（Task 10）交棒前的一瞬静态观感——
 * 渐变底座 + 漂浮粒子 + 初始进度环（[INITIAL_RING_PROGRESS]），不挂 ViewModel，
 * 交棒后由导航壳挂载真首页，Crossfade 全程无跳切。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun HomeSeedPreview(modifier: Modifier = Modifier) {
    val spec = currentThemeSpec()
    Box(modifier) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        FloatingParticles(colors = spec.particleColors, modifier = Modifier.matchParentSize())
        ProgressRing(
            progress = INITIAL_RING_PROGRESS,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(HOME_RING_DIAMETER),
            onRingTap = null,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "0ml", color = spec.ringValueText, style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "今日已喝",
                    color = spec.ringValueText.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** 水滴+涟漪+形序的绘制层：主题渐变底座打底，交棒首页无色差。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SplashVisuals(
    frame: SplashFrame,
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        val metrics =
            with(LocalDensity.current) {
                SplashMetrics(
                    dropletRadiusPx = (DROPLET_DIAMETER / 2).toPx(),
                    fallDistancePx = DROPLET_FALL_DISTANCE.toPx(),
                    rippleStartPx = RIPPLE_START_RADIUS.toPx(),
                    ringRadiusPx = HOME_RING_DIAMETER.toPx() / 2f,
                    rippleStrokePx = RIPPLE_STROKE.toPx(),
                )
            }
        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val cx = size.width / 2f
                    val landingY = size.height * LANDING_Y_FRACTION
                    val rippleCenter = Offset(cx, landingY)
                    when (frame.phase) {
                        SplashPhase.DROPLET ->
                            drawDroplet(
                                center = Offset(cx, landingY - metrics.fallDistancePx * (1f - frame.dropletProgress)),
                                radiusPx = metrics.dropletRadiusPx,
                                color = spec.primary,
                            )
                        SplashPhase.RIPPLE -> {
                            drawRipple(rippleCenter, metrics, frame.ripple1Progress, spec.primary)
                            drawRipple(rippleCenter, metrics, frame.ripple2Progress, spec.primary)
                            // 水滴化作涟漪核心：随第一圈同步收小。
                            drawDroplet(
                                center = rippleCenter,
                                radiusPx = metrics.dropletRadiusPx * (1f - frame.ripple1Progress),
                                color = spec.primary,
                            )
                        }
                        SplashPhase.MORPH ->
                            drawRingSeed(
                                center = rippleCenter,
                                radiusPx = metrics.ringRadiusPx,
                                progress = frame.morphProgress,
                                spec = spec,
                            )
                        SplashPhase.DONE -> Unit
                    }
                },
        )
    }
}

/** 像素度量一次换算，绘制帧间零分配。 */
private data class SplashMetrics(
    val dropletRadiusPx: Float,
    val fallDistancePx: Float,
    val rippleStartPx: Float,
    val ringRadiusPx: Float,
    val rippleStrokePx: Float,
)

private fun DrawScope.drawDroplet(
    center: Offset,
    radiusPx: Float,
    color: Color,
) {
    if (radiusPx <= 0f) return
    drawCircle(color = color, radius = radiusPx, center = center)
}

private fun DrawScope.drawRipple(
    center: Offset,
    metrics: SplashMetrics,
    progress: Float,
    color: Color,
) {
    if (progress <= 0f || progress >= 1f) return
    drawCircle(
        color = color.copy(alpha = (1f - progress) * RIPPLE_MAX_ALPHA),
        radius = lerp(metrics.rippleStartPx, metrics.ringRadiusPx, progress),
        center = center,
        style = Stroke(width = metrics.rippleStrokePx, cap = StrokeCap.Round),
    )
}

/** 涟漪外圈定格为进度环初始态：轨道全圆淡入，主色值弧随形序铺开（与占位首页同观感）。 */
private fun DrawScope.drawRingSeed(
    center: Offset,
    radiusPx: Float,
    progress: Float,
    spec: ThemeSpec,
) {
    if (progress <= 0f) return
    val alpha = progress.coerceIn(0f, 1f)
    val radius = lerp(radiusPx * 0.92f, radiusPx, progress)
    val stroke = radiusPx * RING_STROKE_FRACTION
    val inset = radius - stroke / 2f
    drawCircle(
        color = spec.ringTrack.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawArc(
        color = spec.primary.copy(alpha = alpha),
        startAngle = -90f,
        sweepAngle = INITIAL_RING_PROGRESS * 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - inset, center.y - inset),
        size = Size(inset * 2f, inset * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}
