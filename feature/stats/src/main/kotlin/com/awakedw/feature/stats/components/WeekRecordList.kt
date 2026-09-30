package com.awakedw.feature.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.WeekBar
import com.awakedw.feature.stats.StatsMath

/**
 * 周记录列表（2.5.0 去图表化）：近七日每天一行发丝行——今天在最上，日期称呼（今天/昨天/M月D日）
 * 在左、毫升数在右，行间只压一道发丝线。与今日时间线同一排版语言：
 * 量就摆在那里，不画柱、不画目标线、不做选中读数——记录翻起来像日记，不像报表。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun WeekRecordList(
    bars: List<WeekBar>,
    todayKey: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Column(modifier = modifier.fillMaxWidth()) {
        // 仓储契约：bars 升序且末列为今天；记录列表倒序翻阅，今天先看。
        bars.asReversed().forEachIndexed { index, bar ->
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(spec.laceColor.copy(alpha = 0.22f)))
            }
            WeekRecordRow(
                label = StatsMath.weekRowLabel(bar.dayKey, todayKey),
                totalMl = bar.totalMl,
                isToday = bar.dayKey == todayKey,
                spec = spec,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun WeekRecordRow(
    label: String,
    totalMl: Int,
    isToday: Boolean,
    spec: ThemeSpec,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ControlMinHeight)
                .semantics { contentDescription = "$label，${totalMl}ml" }
                .padding(horizontal = 2.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (isToday) spec.primary else spec.greetingSubColor,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${totalMl}ml",
            color = if (isToday) spec.greetingColor else spec.greetingSubColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
