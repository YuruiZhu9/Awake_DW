package com.awakedw.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.currentThemeSpec

/**
 * 首页主操作组：把立即记录和两个快捷饮量放在同一张轻纸面里。
 * 这是构图层级，不是新的功能入口；所有动作仍然直接写入饮水记录。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun HomeActionDeck(
    cupMl: Int,
    onLog: () -> Unit,
    onQuickLog: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = spec.chipBg.copy(alpha = 0.30f),
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = 0.44f)),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(18.dp)
                            .height(1.dp)
                            .background(spec.laceColor.copy(alpha = 0.72f)),
                )
                Text(
                    text = "记录饮水",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                )
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .background(spec.primary.copy(alpha = 0.72f), CircleShape),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "一杯 ${cupMl}ml",
                    color = spec.greetingSubColor.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            LogButton(onTap = onLog)
            QuickSipsRow(cupMl = cupMl, onQuickLog = onQuickLog)
        }
    }
}
