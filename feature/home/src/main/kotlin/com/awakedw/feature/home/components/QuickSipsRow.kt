package com.awakedw.feature.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.artworkPanelOpacity

/** 快捷胶囊圆角：全圆，与徽章/按钮同一语言。 */
private val QUICK_SIP_SHAPE: Shape = RoundedCornerShape(16.dp)

/** 快捷量档位（§11.1）：小口约半杯、一杯半约 1.5 杯，毫升数取 10 的倍数。 */
internal fun sipAmount(cupMl: Int): Int = roundTo10(cupMl / 2)

internal fun cupAndHalfAmount(cupMl: Int): Int = roundTo10(cupMl * 3 / 2)

private fun roundTo10(v: Int): Int = (v + 5) / 10 * 10

/** 快捷胶囊文案（与「一杯容量」对齐）：讲清各自相当于几杯，不让毫升数孤零零地悬着。 */
internal fun sipLabel(cupMl: Int): String = "小口 ${sipAmount(cupMl)}ml"

internal fun cupAndHalfLabel(cupMl: Int): String = "一杯半 ${cupAndHalfAmount(cupMl)}ml"

/**
 * 快捷饮量行（§11.1）：「小口 {n}ml / 一杯半 {n}ml」两枚次级胶囊，
 * 点按即以该量记一笔（与主按钮共用防抖闸门与环心确认），轻触感同主按钮。
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickSipChip(
            label = sipLabel(cupMl),
            spec = spec,
            modifier = Modifier.weight(1f),
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onQuickLog(sipAmount(cupMl))
            },
        )
        QuickSipChip(
            label = cupAndHalfLabel(cupMl),
            spec = spec,
            modifier = Modifier.weight(1f),
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onQuickLog(cupAndHalfAmount(cupMl))
            },
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun QuickSipChip(
    label: String,
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = QUICK_SIP_SHAPE,
        color = spec.chipBg.copy(alpha = artworkPanelOpacity(spec.id, 0.72f)),
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = 0.52f)),
        onClick = onClick,
        modifier = modifier.heightIn(min = ControlMinHeight),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = null,
                tint = spec.primary.copy(alpha = 0.82f),
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = label,
                color = spec.chipText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                modifier = Modifier.weight(1f, fill = false).padding(start = 6.dp),
            )
        }
    }
}
