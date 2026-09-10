package com.awakedw.feature.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.artworkPanelOpacity

/** 撤回动作的无障碍标签，长按与屏幕阅读器共用同一措辞。 */
internal const val REVERT_LAST_CUP_LABEL = "撤回这一杯"

/**
 * 首页事实摘要：把原先三枚高频胶囊收敛成一张安静的纸面数据条。
 * 仍然只表达今日杯数、最近一杯和平均间隔，不承载连续、奖励或收藏语义。
 *
 * [onRevertLast] 非空时长按「最近一杯」可撤回刚记的那一杯——
 * 记录是自己按下的，就应当可以自己收回；副标题里说明长按可撤回，避免隐藏手势无从发现。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun BadgesRow(
    cupCount: Int,
    avgIntervalLabel: String,
    lastDrinkLabel: String?,
    modifier: Modifier = Modifier,
    onRevertLast: (() -> Unit)? = null,
) {
    val spec = currentThemeSpec()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = spec.chipBg.copy(alpha = artworkPanelOpacity(spec.id, 0.26f)),
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = 0.34f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FactItem(
                label = "今日",
                value = "$cupCount 杯",
                description = "今日 $cupCount 杯",
                modifier = Modifier.weight(1f),
            )
            if (lastDrinkLabel != null) {
                val revert = onRevertLast
                FactItem(
                    label = "最近一杯",
                    value = lastDrinkLabel,
                    description =
                        if (revert == null) {
                            "最近一杯 $lastDrinkLabel"
                        } else {
                            "最近一杯 $lastDrinkLabel，长按可$REVERT_LAST_CUP_LABEL"
                        },
                    onLongClick = revert,
                    modifier = Modifier.weight(1f),
                )
            }
            FactItem(
                label = "平均间隔",
                value = avgIntervalLabel,
                description = "平均间隔 $avgIntervalLabel",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun FactItem(
    label: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val longPressModifier =
        if (onLongClick == null) {
            Modifier
        } else {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClickLabel = REVERT_LAST_CUP_LABEL,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                },
            )
        }
    Column(
        modifier =
            modifier
                .then(longPressModifier)
                // 长按撤回是这一行唯一的隐藏手势：两行文字的天然高度只有 ~40dp，
                // 必须显式补到 ControlMinHeight，否则长按目标偏小、容易按空。
                .heightIn(min = ControlMinHeight)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (onLongClick != null) {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(REVERT_LAST_CUP_LABEL) {
                                    onLongClick()
                                    true
                                },
                            )
                    }
                },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = currentThemeSpec().greetingSubColor,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = currentThemeSpec().chipText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
