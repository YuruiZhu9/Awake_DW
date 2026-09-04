package com.awakedw.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.designsystem.components.IntervalLabel
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 徽章缺省文案：无平均间隔可言时显示破折号。 */
private const val DASH_LABEL = "—"

/** 目标量缺省值：与 UserSettings 默认一致，仅作首帧占位。 */
private const val DEFAULT_GOAL_ML = 1600

/** 周柱状图窗口（天）：含今天。 */
private const val WEEK_DAYS = 7

/** 徽章行三枚 chip 的数据（规格 §3.3 第 1 条）。 */
data class StatsBadges(
    val totalMl: Int,
    val cupCount: Int,
    val avgIntervalLabel: String,
)

/**
 * 统计页一屏状态（规格 §3.3）：徽章行 + 本周柱状图 + 今日时间线。
 * [bars] 末列为今天（仓储契约：weekBars 含今天）；[timeline] 为空时页面展示空态文案。
 */
data class StatsUiState(
    val badges: StatsBadges = StatsBadges(totalMl = 0, cupCount = 0, avgIntervalLabel = DASH_LABEL),
    val bars: List<WeekBar> = emptyList(),
    val goalMl: Int = DEFAULT_GOAL_ML,
    val timeline: List<WaterRecord> = emptyList(),
)

/**
 * Statistics state holder. It exposes current totals, weekly history, and today's timeline.
 * No progression or reward state is part of the statistics screen.
 */
@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val clock: AppClock,
        private val water: WaterRepository,
        prefs: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StatsUiState())

        val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                combine(water.changes, prefs.settings) { _, settings -> settings }
                    .collect { settings -> refresh(settings.goalMl) }
            }
        }

        private suspend fun refresh(goalMl: Int) {
            val stats = water.todayStats()
            _uiState.update {
                it.copy(
                    badges =
                        StatsBadges(
                            totalMl = stats.totalMl,
                            cupCount = stats.cupCount,
                            avgIntervalLabel = IntervalLabel.format(stats.avgIntervalMin),
                        ),
                    bars = water.weekBars(daysBack = WEEK_DAYS),
                    goalMl = goalMl,
                    timeline = water.todayRecords().filter { it.dayKeyLocal == todayKey() },
                )
            }
        }

        private fun todayKey(): String = clock.nowEpochMs().toDayKey(clock.zone())
    }
