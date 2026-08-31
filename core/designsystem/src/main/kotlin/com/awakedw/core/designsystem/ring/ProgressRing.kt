package com.awakedw.core.designsystem.ring

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import com.awakedw.core.designsystem.currentThemeSpec

/** 弧线宽度占环最短边长的比例。 */
private const val STROKE_FRACTION = 0.085f

/** 进度弧起点：正上方（12 点方向）。 */
private const val START_ANGLE_DEGREES = -90f

/** 按压缩放比例：轻按即有回弹，温柔不打扰。 */
private const val PRESS_SCALE = 0.97f

/** 进度变化跟随缓动时长（ms）。 */
private const val PROGRESS_TWEEN_MS = 600

/**
 * 治愈系进度环（设计规格首页核心件）。
 *
 * - 轨道为整圆，取 [com.awakedw.core.designsystem.ThemeSpec.ringTrack]；
 *   值弧走主色 primary；圆头笔触、自顶部顺时针铺开；
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
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = PROGRESS_TWEEN_MS, easing = FastOutSlowInEasing),
        label = "ringProgress",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "ringPressScale",
    )

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
                .drawBehind {
                    val minSide = minOf(size.width, size.height)
                    val stroke = STROKE_FRACTION * minSide
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
                        color = spec.primary,
                        startAngle = START_ANGLE_DEGREES,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcBounds,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
