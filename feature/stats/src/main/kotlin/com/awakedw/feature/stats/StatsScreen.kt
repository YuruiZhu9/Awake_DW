package com.awakedw.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.PagePadding
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.components.PaperPanel
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.LolitaBackdrop
import com.awakedw.core.designsystem.lolita.LolitaRule
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.feature.stats.components.TodayTimeline
import com.awakedw.feature.stats.components.WeekBarsChart

/** 本页漂浮粒子的随机种子：与首页/设置页各不相同，保证各屏粒子排布有别。 */
private const val STATS_PARTICLE_SEED = 11L

/** Statistics screen: current facts, weekly history, and today's timeline. */
@Suppress("ktlint:standard:function-naming")
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val spec = currentThemeSpec()

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        LolitaBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        FloatingParticles(
            colors = spec.particleColors,
            modifier = Modifier.matchParentSize(),
            seed = STATS_PARTICLE_SEED,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PagePadding),
        ) {
            Spacer(Modifier.height(44.dp))
            FadeUpOnce {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "统计",
                        color = spec.greetingColor,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "记录、趋势与今天的饮水时间线",
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            LolitaRule(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
            Spacer(Modifier.height(12.dp))
            FadeUpOnce(delayMillis = 60) {
                PaperPanel(title = "今日概览") {
                    StatsOverview(badges = state.badges, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(20.dp))
            PaperPanel(title = "近七日") {
                WeekBarsChart(bars = state.bars, goalMl = state.goalMl, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
            PaperPanel(title = "今日记录") {
                TodayTimeline(records = state.timeline, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

/**
 * Statistics facts use the same quiet label/value treatment as the home page,
 * so the chart and timeline remain the visual focus instead of a row of pills.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatsOverview(
    badges: StatsBadges,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatsFact(
            label = "今日饮水",
            value = "${badges.totalMl}ml",
            description = "今日饮水 ${badges.totalMl}ml",
        )
        StatsFact(
            label = "今日记录",
            value = "${badges.cupCount} 杯",
            description = "今日记录 ${badges.cupCount} 杯",
        )
        StatsFact(
            label = "平均间隔",
            value = badges.avgIntervalLabel,
            description = "平均间隔 ${badges.avgIntervalLabel}",
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatsFact(
    label: String,
    value: String,
    description: String,
) {
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = currentThemeSpec().greetingSubColor,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
        )
        Text(
            text = value,
            color = currentThemeSpec().chipText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
