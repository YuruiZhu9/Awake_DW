package com.awakedw.feature.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec

/** 快捷胶囊圆角：全圆，与徽章/按钮同一语言。 */
private val QUICK_SIP_SHAPE: Shape = RoundedCornerShape(percent = 50)

/** 快捷量档位（§11.1）：小口约半杯、满杯约一杯半，毫升数取 10 的倍数。 */
internal fun sipAmount(cupMl: Int): Int = roundTo10(cupMl / 2)

internal fun fullAmount(cupMl: Int): Int = roundTo10(cupMl * 3 / 2)

private fun roundTo10(v: Int): Int = (v + 5) / 10 * 10

/**
 * 快捷饮量行（§11.1）：「小口 {n}ml / 满杯 {n}ml」两枚次级胶囊，
 * 点按即以该量记一笔（与主按钮共用防抖闸门与夸夸语反馈），轻触感同主按钮。
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickSipChip(
            label = "小口 ${sipAmount(cupMl)}ml",
            spec = spec,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onQuickLog(sipAmount(cupMl))
            },
        )
        QuickSipChip(
            label = "满杯 ${fullAmount(cupMl)}ml",
            spec = spec,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onQuickLog(fullAmount(cupMl))
            },
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun QuickSipChip(
    label: String,
    spec: ThemeSpec,
    onClick: () -> Unit,
) {
    Surface(
        shape = QUICK_SIP_SHAPE,
        color = spec.chipBg,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = spec.chipText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}
