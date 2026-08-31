package com.awakedw.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.designsystem.components.IntervalLabel
import com.awakedw.core.domain.LogResult
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.model.ThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** 打卡防抖窗口（规格 §4.1）：窗口内经任一入口的连续触发只记一杯。 */
const val LOG_DEBOUNCE_MS = 800L

/** 夸夸语浮现停留时长（规格 §4.2 第 5 步：约 1.4s 后淡出）。 */
const val PRAISE_HOLD_MS = 1_400L

/** 达成庆祝横幅停留时长（任务规格：2500ms 自动收敛）。 */
const val CELEBRATION_HOLD_MS = 2_500L

/**
 * 首页一屏状态。[progress] 已截断到 0..1（达标即满环，微光呼吸交给表现层）；
 * [praiseLine] 为 null 时隐藏；[celebrating] 仅当日首次达标为 true（规格 §4.2 第 6 步）；
 * [greeting] 为 null 表示文案库首抽未就绪，表现层回落时段默认句；
 * [cupMl]/[streakDays]/[lastDrinkLabel] 供快捷量 chips 与徽章行展示（§11.1/11.2）。
 */
data class HomeUiState(
    val themeId: ThemeId = ThemeId.EMERALD,
    val progress: Float = 0f,
    val totalMl: Int = 0,
    val goalMl: Int = 1600,
    val cupMl: Int = 250,
    val cupCount: Int = 0,
    val avgIntervalLabel: String = "—",
    val streakDays: Int = 0,
    val lastDrinkLabel: String? = null,
    val greeting: String? = null,
    val praiseLine: String? = null,
    val celebrating: Boolean = false,
)

/**
 * 治愈打卡首页 ViewModel。
 *
 * - 快照流（统计/目标/主题）单向灌入 [HomeUiState] 的持久字段；
 * - 打卡两入口（按钮/环区）共用同一 800ms 前沿闸门（规格 §4.1「按钮=立即记录」）：
 *   首触立即成笔，环推进/数字滚动/夸夸语随即重叠展开（§4.2）；
 *   距上次成笔不足 800ms 的连点合并忽略；
 * - 打卡成功后按当前时段抽一句夸夸语，1.4s 后收起；celebrated=true 时庆祝态撑满 2.5s，
 *   同日后续打卡（use case 返回 false）即时回到普通反馈。
 *
 * 防抖窗由 [logDebounceMs] 注入（生产 800ms，测试可缩窗），窗口按 [clock] 计量；
 * 成笔后反馈序列不取消，仅以 feedbackEpoch 防串场。
 */
@HiltViewModel
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

    /** 最近一次成笔时刻（epoch ms）：初值取负窗，保证第一次点击立即成笔且不溢出。 */
    private var lastAcceptedAt: Long = -logDebounceMs

    /** 反馈序列代次：新一轮打卡使旧序列的收场动作失效，避免新旧夸夸语互踩。 */
    private var feedbackEpoch = 0

    init {
        // 顶部问候语（设计 §9.2）：进首页即从文案库当前时段组抽一句——
        // 每次新建首页导航条目都会重建 VM，故每次进入都是新的一句（去重池防短期重复）。
        viewModelScope.launch {
            val slot = TimeSlots.slotOfHour(currentHour())
            val greeting = copies.randomFor(slot)
            _uiState.update { it.copy(greeting = greeting) }
        }
        viewModelScope.launch {
            observeHome().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        themeId = snapshot.themeId,
                        progress = (snapshot.stats.totalMl.toFloat() / snapshot.goalMl).coerceIn(0f, 1f),
                        totalMl = snapshot.stats.totalMl,
                        goalMl = snapshot.goalMl,
                        cupMl = snapshot.cupMl,
                        cupCount = snapshot.stats.cupCount,
                        avgIntervalLabel = IntervalLabel.format(snapshot.stats.avgIntervalMin),
                        streakDays = snapshot.streakDays,
                        lastDrinkLabel = snapshot.stats.lastDrankAtEpochMs?.let(::formatTimeOfDay),
                    )
                }
            }
        }
    }

    /** 「记一杯」大按钮：立即记录（规格 §4.1「按钮=立即记录」）。 */
    fun tapLogButton() {
        scheduleLog()
    }

    /** 快捷量入口（§11.1：小口/满杯）：与主按钮共用同一防抖闸门与反馈编排。 */
    fun quickLog(amountMl: Int) {
        scheduleLog(amountMl)
    }

    /** 环区点按记录；[offsetPx] 为环心在环区内的坐标（备用锚点），与按钮共用闸门。 */
    fun tapRing(offsetPx: Offset?) {
        scheduleLog()
    }

    /** 前沿防抖闸门（规格 §4.1）：首触立即成笔；距上次成笔不足 [logDebounceMs] 的触发合并忽略。 */
    private fun scheduleLog(amountMl: Int? = null) {
        val now = clock.nowEpochMs()
        if (now - lastAcceptedAt < logDebounceMs) return
        lastAcceptedAt = now
        viewModelScope.launch { logAndPraise(amountMl) }
    }

    private suspend fun logAndPraise(amountMl: Int?) {
        val result = logWater(amountMl) as? LogResult.Logged
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

    /** 「最近一杯」时刻展示（§11.2）：按注入时钟时区格式化为 HH:mm。 */
    private fun formatTimeOfDay(epochMs: Long): String = TIME_OF_DAY.format(Instant.ofEpochMilli(epochMs).atZone(clock.zone()))

    private companion object {
        val TIME_OF_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
