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
import com.awakedw.core.model.CatMood
import com.awakedw.core.model.resolveCatMood
import com.awakedw.core.sound.AwakeSoundPlayer
import com.awakedw.core.sound.SoundEvent
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

/** 猫语气泡停留时长（moodboard §6.2：2.0s 收场，独立于夸夸语的 1.4s）。 */
const val CAT_LINE_HOLD_MS = 2_000L

/** Immutable state for the water logging home screen. */
data class HomeUiState(
    val progress: Float = 0f,
    val totalMl: Int = 0,
    val goalMl: Int = 1600,
    val cupMl: Int = 250,
    val cupCount: Int = 0,
    val avgIntervalLabel: String = "—",
    val lastDrinkLabel: String? = null,
    val greeting: String? = null,
    val praiseLine: String? = null,
    val celebrating: Boolean = false,
    val catMood: CatMood = CatMood.IDLE,
    val catLine: String? = null,
)

/**
 * Home screen state holder for the water tool.
 * The mascot is presentation-only; it has no progression, collection, or reward state.
 * Timing values are injectable so debounce and feedback behavior remain testable.
 */
@HiltViewModel
class HomeViewModel(
    private val clock: AppClock,
    observeHome: ObserveHomeUseCase,
    private val logWater: LogWaterUseCase,
    private val copies: CopyLibraryRepository,
    private val sound: AwakeSoundPlayer,
    private val logDebounceMs: Long = LOG_DEBOUNCE_MS,
    private val catLineHoldMs: Long = CAT_LINE_HOLD_MS,
) : ViewModel() {
    /** Dagger 注入入口：生产以缺省时长委托主构造器（JSR-330 不识别 Kotlin 缺省参数）。 */
    @Inject
    constructor(
        clock: AppClock,
        observeHome: ObserveHomeUseCase,
        logWater: LogWaterUseCase,
        copies: CopyLibraryRepository,
        sound: AwakeSoundPlayer,
    ) : this(
        clock,
        observeHome,
        logWater,
        copies,
        sound,
        LOG_DEBOUNCE_MS,
        CAT_LINE_HOLD_MS,
    )

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** 最近一次成笔时刻（epoch ms）：初值取负窗，保证第一次点击立即成笔且不溢出。 */
    private var lastAcceptedAt: Long = -logDebounceMs

    /** 反馈序列代次：新一轮打卡使旧序列的收场动作失效，避免新旧夸夸语互踩。 */
    private var feedbackEpoch = 0

    /** 猫序列代次（气泡 + 心情）：摸猫/新打卡换代使旧猫序列的收场失效；独立于 [feedbackEpoch]。 */
    private var catEpoch = 0

    init {
        _uiState.update { it.copy(catMood = resolveCatMood(justCelebrated = false, nowHour = currentHour())) }
        viewModelScope.launch {
            val slot = TimeSlots.slotOfHour(currentHour())
            val greeting = copies.randomFor(slot)
            _uiState.update { it.copy(greeting = greeting) }
        }
        viewModelScope.launch {
            observeHome().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        progress = (snapshot.stats.totalMl.toFloat() / snapshot.goalMl).coerceIn(0f, 1f),
                        totalMl = snapshot.stats.totalMl,
                        goalMl = snapshot.goalMl,
                        cupMl = snapshot.cupMl,
                        cupCount = snapshot.stats.cupCount,
                        avgIntervalLabel = IntervalLabel.format(snapshot.stats.avgIntervalMin),
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

    /** 摸猫：戳一下胆大王，抽一句猫语回应（同 [CAT_LINE_HOLD_MS] 收场，心情不动）+ 一声呼噜。 */
    fun petCat() {
        sound.play(SoundEvent.PURR)
        playCatResponse(happy = false)
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

        // 打卡成功即推进猫序列（moodboard §6.2）：HAPPY 一次 + 抽一句猫语，回应每次成笔。
        if (result != null) {
            // 声音三触发点之一（任务 12）：成笔确认即随机一声掉落音；当日首次达标再追一段旋律。
            // fire-and-forget，与动画解耦——不等夸夸语/庆祝的任何一拍。
            sound.play(DROP_EVENTS.random())
            if (result.celebrated) sound.play(SoundEvent.GOAL_MELODY)
            playCatResponse(happy = true)
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

    /**
     * 猫回应序列（moodboard §6.2）：抽一句猫语点亮气泡，[happy] 时（打卡场景）同时升 HAPPY；
     * [catLineHoldMs] 后收场——气泡清空、心情按当前小时落回（白天 IDLE / 深夜安睡，零惩罚）。
     * 以独立 [catEpoch] 防串场：摸猫/新打卡只换代猫自己，不殃及夸夸语/庆祝的收场。
     */
    private fun playCatResponse(happy: Boolean) {
        catEpoch += 1
        val epoch = catEpoch
        viewModelScope.launch {
            val line = copies.randomCatLine()
            _uiState.update {
                it.copy(catLine = line, catMood = if (happy) CatMood.HAPPY else it.catMood)
            }
            delay(catLineHoldMs)
            if (catEpoch == epoch) {
                _uiState.update {
                    it.copy(
                        catLine = null,
                        catMood = resolveCatMood(justCelebrated = false, nowHour = currentHour()),
                    )
                }
            }
        }
    }

    private fun currentHour(): Int = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.nowEpochMs()), clock.zone()).hour

    /** 「最近一杯」时刻展示（§11.2）：按注入时钟时区格式化为 HH:mm。 */
    private fun formatTimeOfDay(epochMs: Long): String = TIME_OF_DAY.format(Instant.ofEpochMilli(epochMs).atZone(clock.zone()))

    private companion object {
        val TIME_OF_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        /** 掉落音三档（任务 12）：打卡成笔随机抽其一——同一颗水滴听三遍不重样。 */
        val DROP_EVENTS = listOf(SoundEvent.DROP_A, SoundEvent.DROP_B, SoundEvent.DROP_C)
    }
}
