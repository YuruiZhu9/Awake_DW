package com.awakedw.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.domain.GetStreakUseCase
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** 徽章缺省文案：无平均间隔可言时显示破折号。 */
private const val DASH_LABEL = "—"

/** 目标量缺省值：与 UserSettings 默认一致，仅作首帧占位。 */
private const val DEFAULT_GOAL_ML = 1600

/** 低于该分钟数用「X 分钟」文案，否则折叠为小时（与首页一致）。 */
private const val HOURLY_LABEL_MIN = 90

/** 周柱状图窗口（天）：含今天。 */
private const val WEEK_DAYS = 7

/** 徽章行三枚 chip 的数据（规格 §3.3 第 1 条）。 */
data class StatsBadges(
    val cupCount: Int,
    val avgIntervalLabel: String,
    val streakDays: Int,
)

/**
 * 统计页一屏状态（规格 §3.3）：徽章行 + 本周柱状图 + 今日时间线。
 * [bars] 末列为今天（仓储契约：weekBars 含今天）；[timeline] 为空时页面展示空态文案。
 */
data class StatsUiState(
    val badges: StatsBadges = StatsBadges(cupCount = 0, avgIntervalLabel = DASH_LABEL, streakDays = 0),
    val bars: List<WeekBar> = emptyList(),
    val goalMl: Int = DEFAULT_GOAL_ML,
    val timeline: List<WaterRecord> = emptyList(),
)

/**
 * 统计页 ViewModel。
 *
 * 以水库变更流与设置流任一触发即整屏重算——首页打卡、设置页改目标，回到统计页都是最新值；
 * 连胜徽章经真实 [GetStreakUseCase] 穿透，尾端随今日实时达标翻转。
 */
class StatsViewModel
    @Inject
    constructor(
        private val clock: AppClock,
        private val water: WaterRepository,
        prefs: UserPreferencesRepository,
        private val streak: GetStreakUseCase,
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
                            cupCount = stats.cupCount,
                            avgIntervalLabel = avgIntervalLabel(stats.avgIntervalMin),
                            streakDays = streak(),
                        ),
                    bars = water.weekBars(daysBack = WEEK_DAYS),
                    goalMl = goalMl,
                    timeline = water.todayRecords().filter { it.dayKeyLocal == todayKey() },
                )
            }
        }

        private fun todayKey(): String = clock.nowEpochMs().toDayKey(clock.zone())

        /**
         * 平均间隔徽章文案：复刻首页语义——杯数 <2 无平均间隔可言显示「—」；
         * 不足 90 分钟用「X 分钟」，否则折叠为小时（如 1.6h）。
         */
        private fun avgIntervalLabel(avgIntervalMin: Int?): String =
            when {
                avgIntervalMin == null -> DASH_LABEL
                avgIntervalMin < HOURLY_LABEL_MIN -> "$avgIntervalMin 分钟"
                else -> String.format(Locale.US, "%.1fh", avgIntervalMin / 60.0)
            }
    }
