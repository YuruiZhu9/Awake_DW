package com.awakedw.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.designsystem.components.IntervalLabel
import com.awakedw.core.domain.DeleteWaterRecordUseCase
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
import java.time.Instant
import java.time.LocalDate
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
 * 0.9.0 起携带近七日摘要：[weekTotalMl] 合计量与 [weekMetDays] 达标天数（均由 [bars] 与目标推导）。
 * 2.1.0 档案化起携带 [monthCells]/[monthSummary]/[monthLabel]（本月热力）与
 * [rhythm]/[rhythmSummary]（近七日时段节律），均为纯事实派生，无成就语义。
 */
data class StatsUiState(
    val badges: StatsBadges = StatsBadges(totalMl = 0, cupCount = 0, avgIntervalLabel = DASH_LABEL),
    val bars: List<WeekBar> = emptyList(),
    val goalMl: Int = DEFAULT_GOAL_ML,
    val timeline: List<WaterRecord> = emptyList(),
    val weekTotalMl: Int = 0,
    val weekMetDays: Int = 0,
    val monthCells: List<MonthCell?> = emptyList(),
    val monthLabel: String = "",
    val monthSummary: String = "",
    val rhythm: List<RhythmSlice> = emptyList(),
    val rhythmSummary: String = "",
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
        private val deleteWater: DeleteWaterRecordUseCase,
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

        /** 删除某一笔记录（时间线长按纠错）：删除后由变更流自动重算本页全部数字。 */
        fun deleteRecord(recordId: Long) {
            viewModelScope.launch { deleteWater(recordId) }
        }

        private suspend fun refresh(goalMl: Int) {
            val stats = water.todayStats()
            val bars = water.weekBars(daysBack = WEEK_DAYS)
            val today = todayDate()
            // 本月热力复用 weekBars：daysBack = 今天几号，恰好覆盖本月 1 日至今（见 2.1.0 计划 §2.6）。
            val monthTotals =
                water.weekBars(daysBack = today.dayOfMonth).associate { it.dayKey to it.totalMl }
            val monthCells = ArchiveMath.monthCells(todayKey(), monthTotals)
            val rhythm = ArchiveMath.rhythmOf(water.recentRecords(daysBack = WEEK_DAYS), clock.zone())
            _uiState.update {
                it.copy(
                    badges =
                        StatsBadges(
                            totalMl = stats.totalMl,
                            cupCount = stats.cupCount,
                            avgIntervalLabel = IntervalLabel.format(stats.avgIntervalMin),
                        ),
                    bars = bars,
                    goalMl = goalMl,
                    timeline = water.todayRecords().filter { it.dayKeyLocal == todayKey() },
                    weekTotalMl = bars.sumOf { it.totalMl },
                    weekMetDays = bars.count { it.totalMl >= goalMl },
                    monthCells = monthCells,
                    monthLabel = ArchiveMath.monthTitle(todayKey()),
                    monthSummary = ArchiveMath.monthSummary(monthCells, goalMl),
                    rhythm = rhythm,
                    rhythmSummary = ArchiveMath.rhythmSummary(rhythm),
                )
            }
        }

        private fun todayDate(): LocalDate = Instant.ofEpochMilli(clock.nowEpochMs()).atZone(clock.zone()).toLocalDate()

        private fun todayKey(): String = clock.nowEpochMs().toDayKey(clock.zone())
    }
