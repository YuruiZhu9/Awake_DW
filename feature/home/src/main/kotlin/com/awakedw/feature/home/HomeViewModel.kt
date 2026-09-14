package com.awakedw.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.designsystem.components.IntervalLabel
import com.awakedw.core.domain.DeleteWaterRecordUseCase
import com.awakedw.core.domain.LogResult
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.model.CatMood
import com.awakedw.core.model.TimeSlot
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

/**
 * 环心打卡确认停留时长（视觉基线 §12.2）：引文要读完正文与落款两行，
 * 停留短了会两行都没读完就消失——这正是「不知道该看哪里」的成因之一。
 */
const val PRAISE_HOLD_MS = 3_000L

/** 防抖窗口内重复触发时的微提示停留时长：短于打卡引文，只为说明「刚才那下已记过」。 */
const val REPEAT_HINT_HOLD_MS = 900L

/** 达标反馈状态停留时长（2500ms 自动收敛，不产生奖励或内容解锁）。 */
const val CELEBRATION_HOLD_MS = 2_500L

/**
 * 猫气泡停留时长：与环心打卡确认同拍（视觉基线 §12.2）。
 * 两者同时出现、同时收场，任一刻画面上只有一段新文字是一等公民。
 */
const val CAT_LINE_HOLD_MS = 3_000L

/** 防抖窗口内重复触发时的提示语：不是报错，只是把「已经记过」说清楚。 */
const val REPEAT_HINT_TEXT = "刚刚记过了"

/** 撤回成功后的确认语前缀：删掉的是哪一杯，要说清楚，不能默默把数字改小。 */
const val REVERT_ACK_PREFIX = "已撤回 · "

/**
 * 环心那一行临时文案：打卡引文（可带落款），或重复提示／撤回回执这类操作说明（无落款）。
 *
 * 打卡引文与操作提示共用同一个位置是有意的——它们都发生在使用者刚看着的环心；
 * 用同一种数据形态表达，环心只需要一套渲染与一套横切动画。
 */
data class RingNote(
    val text: String,
    val attribution: String? = null,
)

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
    /**
     * 环心确认行（打卡引文或操作提示）；null 时环心显示默认的「今日已喝」。
     * 与猫咪气泡物理分离：确认发生在刚被看着的位置，猫语留在猫那一行，两者不再抢同一格。
     */
    val centerNote: RingNote? = null,
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
    private val deleteWater: DeleteWaterRecordUseCase,
    private val copies: CopyLibraryRepository,
    private val sound: AwakeSoundPlayer,
    private val logDebounceMs: Long = LOG_DEBOUNCE_MS,
    private val catLineHoldMs: Long = CAT_LINE_HOLD_MS,
    private val praiseHoldMs: Long = PRAISE_HOLD_MS,
    private val repeatHintHoldMs: Long = REPEAT_HINT_HOLD_MS,
) : ViewModel() {
    /** Dagger 注入入口：生产以缺省时长委托主构造器（JSR-330 不识别 Kotlin 缺省参数）。 */
    @Inject
    constructor(
        clock: AppClock,
        observeHome: ObserveHomeUseCase,
        logWater: LogWaterUseCase,
        deleteWater: DeleteWaterRecordUseCase,
        copies: CopyLibraryRepository,
        sound: AwakeSoundPlayer,
    ) : this(
        clock,
        observeHome,
        logWater,
        deleteWater,
        copies,
        sound,
        LOG_DEBOUNCE_MS,
        CAT_LINE_HOLD_MS,
        PRAISE_HOLD_MS,
        REPEAT_HINT_HOLD_MS,
    )

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** 最近一次成笔时刻（epoch ms）：初值取负窗，保证第一次点击立即成笔且不溢出。 */
    private var lastAcceptedAt: Long = -logDebounceMs

    /** 反馈序列代次：新一轮打卡使旧序列的收场动作失效，避免新旧反馈互踩。 */
    private var feedbackEpoch = 0

    /** 环心确认行代次：与 [feedbackEpoch] 分开——重复提示不应当取消达标横幅的收场。 */
    private var centerEpoch = 0

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

    /** 快捷量入口（§11.1：小口/一杯半）：与主按钮共用同一防抖闸门与反馈编排。 */
    fun quickLog(amountMl: Int) {
        scheduleLog(amountMl)
    }

    /** 环区点按记录；[offsetPx] 为环心在环区内的坐标（备用锚点），与按钮共用闸门。 */
    fun tapRing(offsetPx: Offset?) {
        scheduleLog()
    }

    /** 摸猫：戳一下胆大王，抽一句短句回应（同 [catLineHoldMs] 收场，心情不动）+ 一声呼噜。 */
    fun petCat() {
        sound.play(SoundEvent.PURR)
        playCatResponse(happy = false, slot = TimeSlots.slotOfHour(currentHour()))
    }

    /**
     * 撤回今日最后一杯（首页「最近一杯」长按）：
     * 删除后由仓储变更流驱动进度、事实条与统计页同步重算，本层不做本地推算。
     * 成功后走环心确认通道回一句「已撤回 · {n}ml」——删除是破坏性动作，
     * 不能让数字默默变小而没有任何交代。
     */
    fun revertLatestCup() {
        viewModelScope.launch {
            val removed = deleteWater.revertLatestToday()
            if (removed != null) {
                showCenterNote(RingNote("$REVERT_ACK_PREFIX${removed.amountMl}ml"), praiseHoldMs)
            }
        }
    }

    /** 前沿防抖闸门（规格 §4.1）：首触立即成笔；窗口内的后续触发合并忽略，但给一句明确回显。 */
    private fun scheduleLog(amountMl: Int? = null) {
        val now = clock.nowEpochMs()
        if (now - lastAcceptedAt < logDebounceMs) {
            showCenterNote(RingNote(REPEAT_HINT_TEXT), repeatHintHoldMs)
            return
        }
        lastAcceptedAt = now
        viewModelScope.launch { logAndPraise(amountMl) }
    }

    private suspend fun logAndPraise(amountMl: Int?) {
        val result = logWater(amountMl) as? LogResult.Logged
        feedbackEpoch += 1
        val epoch = feedbackEpoch

        val slot = TimeSlots.slotOfHour(currentHour())
        // 环心确认：引文池，与猫语各自独立去重。打卡瞬间读到的是一句引文和它的落款，
        // 而不是一句自我评价（视觉基线 §12.1）。
        val quote = copies.randomPraise(slot)
        showCenterNote(RingNote(text = quote.text, attribution = quote.attribution), praiseHoldMs)
        _uiState.update {
            it.copy(
                // 当日首次达标为 true；其余打卡（含达标后再打）一律回到普通反馈。
                celebrating = result?.celebrated == true,
            )
        }

        // 打卡成功触发一次轻量猫反馈：HAPPY 一次 + 抽一句猫语，回应每次成笔。
        if (result != null) {
            // 声音三触发点之一（任务 12）：成笔确认即随机一声掉落音；当日首次达标再追一段旋律。
            // fire-and-forget，与动画解耦——不等环心确认/庆祝的任何一拍。
            sound.play(DROP_EVENTS.random())
            if (result.celebrated) sound.play(SoundEvent.GOAL_MELODY)
            playCatResponse(happy = true, slot = slot)
        }

        if (result?.celebrated == true) {
            delay(CELEBRATION_HOLD_MS)
            if (feedbackEpoch == epoch) {
                _uiState.update { it.copy(celebrating = false) }
            }
        }
    }

    /** 环心确认行：写入文案并按时收场；后一次调用换代，旧收场自动失效。 */
    private fun showCenterNote(
        note: RingNote,
        holdMs: Long,
    ) {
        centerEpoch += 1
        val epoch = centerEpoch
        _uiState.update { it.copy(centerNote = note) }
        viewModelScope.launch {
            delay(holdMs)
            if (centerEpoch == epoch) {
                _uiState.update { it.copy(centerNote = null) }
            }
        }
    }

    /**
     * 猫回应序列：抽一句猫语点亮气泡，[happy] 时（打卡场景）同时升 HAPPY；
     * [catLineHoldMs] 后收场——气泡清空、心情按当前小时落回（白天 IDLE / 深夜安睡，零惩罚）。
     * 打卡场景下该时长与环心打卡引文同值：两者同时出现、同时收场（视觉基线 §12.2）。
     * 以独立 [catEpoch] 防串场：摸猫/新打卡只换代猫自己，不殃及环心确认与达标横幅的收场。
     */
    private fun playCatResponse(
        happy: Boolean,
        slot: TimeSlot,
    ) {
        catEpoch += 1
        val epoch = catEpoch
        viewModelScope.launch {
            val line = copies.randomCatLine(slot = slot)
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
