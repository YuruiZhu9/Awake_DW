package com.awakedw.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.domain.LogResult
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

/** 打卡防抖窗口（规格 §4.1）：窗口内经任一入口的连续触发只记一杯。 */
const val LOG_DEBOUNCE_MS = 800L

/** 夸夸语浮现停留时长（规格 §4.2 第 5 步：约 1.4s 后淡出）。 */
const val PRAISE_HOLD_MS = 1_400L

/** 达成庆祝横幅停留时长（任务规格：2500ms 自动收敛）。 */
const val CELEBRATION_HOLD_MS = 2_500L

/**
 * 首页一屏状态。[progress] 已截断到 0..1（达标即满环，微光呼吸交给表现层）；
 * [praiseLine] 为 null 时隐藏；[celebrating] 仅当日首次达标为 true（规格 §4.2 第 6 步）。
 */
data class HomeUiState(
    val themeId: ThemeId = ThemeId.EMERALD,
    val progress: Float = 0f,
    val totalMl: Int = 0,
    val goalMl: Int = 1600,
    val cupCount: Int = 0,
    val avgIntervalLabel: String = "—",
    val praiseLine: String? = null,
    val celebrating: Boolean = false,
)

/**
 * 治愈打卡首页 ViewModel。
 *
 * - 快照流（统计/目标/主题）单向灌入 [HomeUiState] 的持久字段；
 * - 打卡两入口（按钮/环区）共用同一 800ms 防抖闸门：窗口内后到的点击取消尚未成笔的那笔；
 * - 打卡成功后按当前时段抽一句夸夸语，1.4s 后收起；celebrated=true 时庆祝态撑满 2.5s，
 *   同日后续打卡（use case 返回 false）即时回到普通反馈。
 *
 * 防抖窗由 [logDebounceMs] 注入（生产 800ms，测试可缩窗）；
 * 成笔之后反馈序列不再受后续点击取消，只以 feedbackEpoch 防串场。
 */
class HomeViewModel(
    private val clock: AppClock,
    observeHome: ObserveHomeUseCase,
    private val logWater: LogWaterUseCase,
    private val copies: CopyLibraryRepository,
    private val logDebounceMs: Long = LOG_DEBOUNCE_MS,
) : ViewModel() {
    /** Dagger 注入入口：生产以默认防抖窗委托主构造器（JSR-330 不识别 Kotlin 缺省参数）。 */
    @Inject
    constructor(
        clock: AppClock,
        observeHome: ObserveHomeUseCase,
        logWater: LogWaterUseCase,
        copies: CopyLibraryRepository,
    ) : this(
        clock,
        observeHome,
        logWater,
        copies,
        LOG_DEBOUNCE_MS,
    )

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var pendingLog: Job? = null

    /** 反馈序列代次：新一轮打卡使旧序列的收场动作失效，避免新旧夸夸语互踩。 */
    private var feedbackEpoch = 0

    init {
        viewModelScope.launch {
            observeHome().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        themeId = snapshot.themeId,
                        progress = (snapshot.stats.totalMl.toFloat() / snapshot.goalMl).coerceIn(0f, 1f),
                        totalMl = snapshot.stats.totalMl,
                        goalMl = snapshot.goalMl,
                        cupCount = snapshot.stats.cupCount,
                        avgIntervalLabel = avgIntervalLabel(snapshot.stats.avgIntervalMin),
                    )
                }
            }
        }
    }

    /** 「记一杯」大按钮：立即记录（进防抖闸门）。 */
    fun tapLogButton() {
        scheduleLog()
    }

    /** 环区点按记录；[offsetPx] 为环心在环区内的坐标（备用锚点），与按钮共用防抖。 */
    fun tapRing(offsetPx: Offset?) {
        scheduleLog()
    }

    private fun scheduleLog() {
        pendingLog?.cancel()
        pendingLog =
            viewModelScope.launch {
                delay(logDebounceMs)
                // 已成笔：置空后后续点击不再能取消这笔的反馈序列。
                pendingLog = null
                logAndPraise()
            }
    }

    private suspend fun logAndPraise() {
        val result = logWater() as? LogResult.Logged
        feedbackEpoch += 1
        val epoch = feedbackEpoch

        val slot = TimeSlots.slotOfHour(currentHour())
        val praise = copies.randomFor(slot)
        _uiState.update {
            it.copy(
                praiseLine = praise,
                // 当日首次达标为 true；其余打卡（含达标后再打）一律回到普通反馈。
                celebrating = result?.celebrated == true,
            )
        }

        delay(PRAISE_HOLD_MS)
        if (feedbackEpoch == epoch) {
            _uiState.update { it.copy(praiseLine = null) }
        }
        if (result?.celebrated == true) {
            delay(CELEBRATION_HOLD_MS - PRAISE_HOLD_MS)
            if (feedbackEpoch == epoch) {
                _uiState.update { it.copy(celebrating = false) }
            }
        }
    }

    private fun currentHour(): Int = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.nowEpochMs()), clock.zone()).hour

    /** 平均间隔徽章文案：杯数 <2 为「—」；<90 分钟用分钟文案，否则小时文案（如 1.6h）。 */
    private fun avgIntervalLabel(avgIntervalMin: Int?): String =
        when {
            avgIntervalMin == null -> "—"
            avgIntervalMin < HOURLY_LABEL_MIN -> "$avgIntervalMin 分钟"
            else -> String.format(Locale.US, "%.1fh", avgIntervalMin / 60.0)
        }

    private companion object {
        /** 低于该分钟数用「X 分钟」文案，否则折叠为小时。 */
        const val HOURLY_LABEL_MIN = 90
    }
}
