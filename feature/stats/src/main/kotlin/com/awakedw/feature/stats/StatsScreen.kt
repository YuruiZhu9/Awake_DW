package com.awakedw.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.PagePadding
import com.awakedw.core.designsystem.components.AwakeConfirmDialog
import com.awakedw.core.designsystem.components.EditorialHeader
import com.awakedw.core.designsystem.components.PaperPanel
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.LolitaBackdrop
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.particles.ParticleDensity
import com.awakedw.core.model.WaterRecord
import com.awakedw.feature.stats.components.TodayTimeline
import com.awakedw.feature.stats.components.WeekBarsChart

@Suppress("ktlint:standard:function-naming")
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    StatsContent(state, onDeleteRecord = viewModel::deleteRecord)
}

/** Pure display entry point for populated, empty and large-font visual regression. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun StatsContent(
    state: StatsUiState,
    onDeleteRecord: (Long) -> Unit = {},
) {
    val spec = currentThemeSpec()
    var pendingDelete by remember { mutableStateOf<WaterRecord?>(null) }
    Box(Modifier.fillMaxSize()) {
        GradientBackdrop(spec, Modifier.matchParentSize())
        LolitaBackdrop(spec, Modifier.matchParentSize())
        FloatingParticles(
            spec.particleColors,
            Modifier.matchParentSize(),
            seed = 11L,
            showFlowers = false,
            showStars = false,
            density = ParticleDensity.QUIET,
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PagePadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            EditorialHeader("统计", "今天的饮水与近七日变化", Icons.Rounded.BarChart)
            PaperPanel {
                Text("今日饮水", color = spec.greetingSubColor, style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(state.badges.totalMl.toString(), color = spec.greetingColor, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "ml",
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (state.badges.totalMl.toFloat() / state.goalMl.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = spec.primary,
                    trackColor = spec.ringTrack,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "每日目标 ${state.goalMl}ml",
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    // 达标是一个事实，不是奖励：只把「已达成」写清楚，不引入徽章或连续语义。
                    if (state.badges.totalMl >= state.goalMl) {
                        Text("· 已达成", color = spec.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatsFact("记录次数", "${state.badges.cupCount} 次", Modifier.weight(1f))
                    StatsFact("平均间隔", state.badges.avgIntervalLabel, Modifier.weight(1f))
                }
            }
            PaperPanel(title = "近七日") {
                WeekBarsChart(state.bars, state.goalMl, Modifier.fillMaxWidth())
            }
            PaperPanel(title = "今日记录 · ${state.timeline.size} 次") {
                TodayTimeline(
                    records = state.timeline,
                    onRequestDelete = { record -> pendingDelete = record },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    pendingDelete?.let { record ->
        AwakeConfirmDialog(
            title = "删除这条记录",
            body = "${record.amountMl}ml 的这条记录会被删掉，今日总数与近七日同步减少。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                onDeleteRecord(record.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatsFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Column(modifier.clearAndSetSemantics { contentDescription = "$label $value" }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = spec.greetingSubColor, style = MaterialTheme.typography.bodySmall)
        Text(value, color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
    }
}
