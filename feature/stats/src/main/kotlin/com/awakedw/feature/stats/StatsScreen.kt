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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.PagePadding
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.components.BadgeChip
import com.awakedw.core.designsystem.components.PaperPanel
import com.awakedw.core.designsystem.currentThemeSpec
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
                Text(
                    text = "统计",
                    color = spec.greetingColor,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            LolitaRule(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
            Spacer(Modifier.height(10.dp))
            FadeUpOnce(delayMillis = 60) { StatsBadgesRow(badges = state.badges, modifier = Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(24.dp))
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

/** Current factual summaries, kept readable on narrow screens. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatsBadgesRow(
    badges: StatsBadges,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BadgeChip(text = "今日 ${badges.cupCount} 杯 ☀", spec = spec)
        BadgeChip(text = "平均间隔 ${badges.avgIntervalLabel} ⏱", spec = spec)
    }
}
