package com.awakedw.feature.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.animation.pressScale
import com.awakedw.core.designsystem.currentThemeSpec

/** 撕票顶边与竖分隔的虚线节奏。 */
private val TICKET_DASH = 3.dp
private val TICKET_DIVIDER_INSET = 5.dp

/** 快捷量档位（§11.1）：小口约半杯、一杯半约 1.5 杯，毫升数取 10 的倍数。 */
internal fun sipAmount(cupMl: Int): Int = roundTo10(cupMl / 2)

internal fun cupAndHalfAmount(cupMl: Int): Int = roundTo10(cupMl * 3 / 2)

/** 就近取整到 10 的倍数；拖环换算（RingDragMath）与快捷量共用同一把刻度。 */
internal fun roundTo10(v: Int): Int = (v + 5) / 10 * 10

/**
 * 快捷饮量（§11.1，大改版 Phase 1 改「页边撕票」）：
 * 两枚票根共享一条虚线顶边，中间以竖虚线撕开；点按即以该量记一笔
 * （与主按钮共用防抖闸门与环心确认），轻触感同主按钮。
 *
 * 票根仍是次级路径：无底色、无描边盒，只靠线与字排进纸面；
 * 触控高度与语义节点保持 ≥48dp，读屏动作补一句「记 {n}ml」。
 *
 * 第二档叫「一杯半」而不是「满杯」：设置里的「一杯容量」已经定义了杯子的大小，
 * 主按钮写的是 +{一杯容量}ml，再叫「满杯」会让同一个杯子出现两种容量。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun QuickSipsRow(
    cupMl: Int,
    onQuickLog: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val view = LocalView.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        QuickTicket(
            label = "小口",
            amountMl = sipAmount(cupMl),
            spec = spec,
            showDivider = false,
            modifier = Modifier.weight(1f),
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onQuickLog(sipAmount(cupMl))
            },
        )
        QuickTicket(
            label = "一杯半",
            amountMl = cupAndHalfAmount(cupMl),
            spec = spec,
            showDivider = true,
            modifier = Modifier.weight(1f),
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onQuickLog(cupAndHalfAmount(cupMl))
            },
        )
    }
}

/** 单枚撕票：虚线顶边 + （可选）左竖虚线，内容为小标签与衬线毫升数。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun QuickTicket(
    label: String,
    amountMl: Int,
    spec: ThemeSpec,
    showDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .heightIn(min = ControlMinHeight)
                .pressScale(interactionSource)
                .drawBehind {
                    val dash =
                        PathEffect.dashPathEffect(
                            floatArrayOf(TICKET_DASH.toPx(), TICKET_DASH.toPx()),
                        )
                    drawLine(
                        color = spec.laceColor.copy(alpha = 0.62f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                    if (showDivider) {
                        drawLine(
                            color = spec.laceColor.copy(alpha = 0.55f),
                            start = Offset(0f, TICKET_DIVIDER_INSET.toPx()),
                            end = Offset(0f, size.height - TICKET_DIVIDER_INSET.toPx()),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash,
                        )
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "记 ${amountMl}ml",
                    onClick = onClick,
                )
                .padding(
                    start = if (showDivider) 14.dp else 2.dp,
                    end = 4.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp, letterSpacing = 1.2.sp),
            )
            Row {
                Text(
                    text = amountMl.toString(),
                    color = spec.chipText,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                        ),
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "ml",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
                    modifier = Modifier.alignByBaseline().padding(start = 2.dp),
                )
            }
        }
    }
}
