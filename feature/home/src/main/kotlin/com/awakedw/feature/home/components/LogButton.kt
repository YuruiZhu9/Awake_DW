package com.awakedw.feature.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.designsystem.animation.pressScale
import com.awakedw.core.designsystem.burst.BurstParticles
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.onPrimarySurface
import com.awakedw.core.designsystem.rememberReduceMotion

/** Button label is deliberately stable: the primary action always means one thing. */
internal const val LOG_BUTTON_LABEL = "记一杯"

/** 撤回动作的可见文案（2.11.0）：10s 窗口内占据尾端「+{cupMl}ml」的位置。 */
internal const val UNDO_ACTION_LABEL = "撤回"

/** 压印长条的近直角外形（Phase 1）：3dp 圆角，与印章/票根的器物语言对齐。 */
private val LOG_BUTTON_SHAPE = RoundedCornerShape(3.dp)

/** 尾端撤回落点的最小宽度与发丝分隔：保证独立可点面 ≥48dp 宽，不挤占主文案。 */
private val UNDO_ACTION_MIN_WIDTH = 56.dp
private val UNDO_DIVIDER_HEIGHT = 20.dp
private const val UNDO_DIVIDER_ALPHA = 0.28f
private const val UNDO_PRESSED_ALPHA = 0.16f

/**
 * Primary water logging action: a letterpress bar (Phase 1 of the editorial overhaul)
 * with a small seal mark, pressed feedback, and a restrained burst that confirms the
 * tap without adding a dialog.
 *
 * 2.11.0 打卡纠错闭环：成笔后的 10s 内，尾端杯量标签换成「撤回」——
 * 动作与记录共享同一条压印长条，不新增文字通道、不推动页面布局。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun LogButton(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    cupMl: Int? = null,
    undoVisible: Boolean = false,
    undoAmountMl: Int? = null,
    onUndo: () -> Unit = {},
) {
    val spec = currentThemeSpec()
    val interactionSource = remember { MutableInteractionSource() }
    val reduceMotion = rememberReduceMotion()
    val ink = onPrimarySurface(spec)

    var buttonSize by remember { mutableStateOf(IntSize.Zero) }
    var burstTrigger by remember { mutableIntStateOf(0) }
    val view = LocalView.current

    Box(
        modifier =
            modifier
                .onSizeChanged { buttonSize = it }
                .pressScale(interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .clip(LOG_BUTTON_SHAPE)
                    .background(Brush.verticalGradient(listOf(spec.buttonTop, spec.buttonBottom)))
                    // 压印语言（Phase 1）：上缘 1dp 内高光、下缘 10dp 内阴影、1dp 外框；
                    // 无模糊、无渐变胶囊——按下去像一枚盖章条。
                    .drawWithCache {
                        val edge = 1.dp.toPx()
                        val bottomShade =
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f)),
                            )
                        onDrawBehind {
                            drawLine(
                                color = Color.White.copy(alpha = 0.22f),
                                start = Offset(0f, edge),
                                end = Offset(size.width, edge),
                                strokeWidth = edge,
                            )
                            drawRect(
                                brush = bottomShade,
                                topLeft = Offset(0f, size.height - 10.dp.toPx()),
                                size = Size(size.width, 10.dp.toPx()),
                            )
                            drawRect(
                                color = Color.White.copy(alpha = 0.18f),
                                style = Stroke(width = edge),
                            )
                        }
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        burstTrigger += 1
                        onTap()
                    }
                    .padding(horizontal = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // 左侧印章：细环 + 水滴，代替原来的圆角方块图标。
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .drawBehind {
                                drawCircle(ink.copy(alpha = 0.10f))
                                drawCircle(
                                    color = ink.copy(alpha = 0.38f),
                                    style = Stroke(width = 1.dp.toPx()),
                                )
                                drawCircle(
                                    color = ink.copy(alpha = 0.16f),
                                    radius = size.minDimension / 2f - 3.dp.toPx(),
                                    style = Stroke(width = 0.7.dp.toPx()),
                                )
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = LOG_BUTTON_LABEL,
                    color = ink,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                        ),
                    modifier = Modifier.padding(start = 11.dp),
                )
                if (undoVisible || cupMl != null) {
                    Spacer(Modifier.weight(1f))
                    Crossfade(
                        targetState = undoVisible,
                        animationSpec =
                            tween(
                                durationMillis = if (reduceMotion) 0 else MotionTokens.DURATION_FEEDBACK_MS,
                                easing = MotionTokens.EasingStandard,
                            ),
                        label = "logButtonTrailing",
                    ) { showingUndo ->
                        if (showingUndo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .width(1.dp)
                                        .height(UNDO_DIVIDER_HEIGHT)
                                        .background(ink.copy(alpha = UNDO_DIVIDER_ALPHA)),
                                )
                                UndoAction(amountMl = undoAmountMl, onUndo = onUndo)
                            }
                        } else if (cupMl != null) {
                            Text(
                                text = "+${cupMl}ml",
                                color = ink.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        } else {
                            Spacer(Modifier.width(0.dp))
                        }
                    }
                }
            }
        }
        BurstParticles(
            origin = Offset(buttonSize.width / 2f, buttonSize.height / 2f),
            colors = spec.particleColors,
            trigger = burstTrigger,
            onFinish = { },
        )
    }
}

/**
 * 胶囊尾端的撤回动作：成笔后 10s 内可一击收回最后一杯，不弹确认。
 * 独立语义节点、独立按压底、最小 48dp 高度；只请求一次轻触觉，不播放声音。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun UndoAction(
    amountMl: Int?,
    onUndo: () -> Unit,
) {
    val spec = currentThemeSpec()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val ink = onPrimarySurface(spec)
    val view = LocalView.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier =
            Modifier
                .heightIn(min = ControlMinHeight)
                .widthIn(min = UNDO_ACTION_MIN_WIDTH)
                .clip(shape)
                .background(if (pressed) ink.copy(alpha = UNDO_PRESSED_ALPHA) else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onUndo()
                }
                .semantics {
                    // 可见文案只有「撤回」，读屏补足对象与量：撤回上一杯，250ml。
                    contentDescription = amountMl?.let { "撤回上一杯，${it}ml" } ?: "撤回上一杯"
                }
                .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = UNDO_ACTION_LABEL,
            color = ink,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
