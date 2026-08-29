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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.components.BadgeChip
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.feature.stats.components.TodayTimeline
import com.awakedw.feature.stats.components.WeekBarsChart

/** 本页漂浮粒子的随机种子：与首页/设置页各不相同，保证各屏粒子排布有别。 */
private const val STATS_PARTICLE_SEED = 11L

/** 页面左右留白（与首页一致）。 */
private val PAGE_HORIZONTAL_PADDING = 24.dp

/**
 * 统计页（规格 §3.3）：给「坚持的痕迹」一个专属空间——
 * 徽章行（今日杯数 · 平均间隔 · 连续达标）、本周柱状图、今日时间线，三模块纵向排列。
 * 背景沿用全局渐变底座（自带柔光晕与噪点颗粒）+ 漂浮粒子，与首页同一份主题呼吸。
 */
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
                    .padding(horizontal = PAGE_HORIZONTAL_PADDING),
        ) {
            Spacer(Modifier.height(44.dp))
            FadeUpOnce { Text(text = "统计", color = spec.greetingColor, style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.height(20.dp))
            FadeUpOnce(delayMillis = 60) { StatsBadgesRow(badges = state.badges, modifier = Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(28.dp))
            WeekBarsChart(bars = state.bars, goalMl = state.goalMl, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
            TodayTimeline(records = state.timeline, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(36.dp))
        }
    }
}

/** 统计三徽章（规格 §3.3 第 1 条）：「今日 {n} 杯 ☀」「平均间隔 {label} ⏱」+ 连胜徽章（0 天隐藏）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatsBadgesRow(
    badges: StatsBadges,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BadgeChip(text = "今日 ${badges.cupCount} 杯 ☀", spec = spec)
        BadgeChip(text = "平均间隔 ${badges.avgIntervalLabel} ⏱", spec = spec)
        if (badges.streakDays >= 2) {
            BadgeChip(text = "连续 ${badges.streakDays} 天 🏅", spec = spec)
        } else if (badges.streakDays == 1) {
            BadgeChip(text = "第 1 天 ✨", spec = spec)
        }
    }
}
