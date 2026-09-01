package com.awakedw.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.designsystem.components.IntervalLabel
import com.awakedw.core.domain.GetStreakUseCase
import com.awakedw.core.domain.LogResult
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveDailyOutfitUseCase
import com.awakedw.core.domain.UnlockOutfitsUseCase
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.model.CatAccessory
import com.awakedw.core.model.CatMood
import com.awakedw.core.model.Outfit
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.resolveCatMood
import com.awakedw.core.model.unlockedCatAccessories
import com.awakedw.core.sound.AwakeSoundPlayer
import com.awakedw.core.sound.SoundEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
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

/** 新解锁轻提示停留时长（任务规格：与庆祝横幅同拍 2500ms 后由 epoch 收场清空）。 */
const val NEW_UNLOCK_HOLD_MS = 2_500L

/** 猫语气泡停留时长（moodboard §6.2：2.0s 收场，独立于夸夸语的 1.4s）。 */
const val CAT_LINE_HOLD_MS = 2_000L

/**
 * 首页一屏状态。[progress] 已截断到 0..1（达标即满环，微光呼吸交给表现层）；
 * [praiseLine] 为 null 时隐藏；[celebrating] 仅当日首次达标为 true（规格 §4.2 第 6 步）；
 * [greeting] 为 null 表示文案库首抽未就绪，表现层回落时段默认句；
 * [cupMl]/[streakDays]/[lastDrinkLabel] 供快捷量 chips 与徽章行展示（§11.1/11.2）；
 * [todayOutfit] 为 null 表示今日之裙解析未就绪，表现层不画卷不显签（moodboard §5.1）；
 * [newUnlock] 为本次打卡新解锁的藏品，浮出轻提示 [NEW_UNLOCK_HOLD_MS] 后收场清空；
 * [catMood] 为胆大王心情三态（moodboard §6，打卡短暂 HAPPY、深夜安睡）；
 * [catLine] 为猫语气泡，浮现 [CAT_LINE_HOLD_MS] 后收场清空（独立于夸夸语位置与节奏）；
 * [catAccessories] 为按连胜解锁的猫配饰（init 与每次打卡成功后刷新）。
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
    val todayOutfit: Outfit? = null,
    val newUnlock: Outfit? = null,
    val catMood: CatMood = CatMood.IDLE,
    val catLine: String? = null,
    val catAccessories: List<CatAccessory> = emptyList(),
)

/**
 * 治愈打卡首页 ViewModel。
 *
 * - 快照流（统计/目标/主题）单向灌入 [HomeUiState] 的持久字段；
 * - 进首页即解析今日之裙（moodboard §5.1）灌入 [HomeUiState.todayOutfit]——画卷层与穿搭签的数据源；
 *   VM 存活期间画廊改钉选不重建本 VM，故对 pin 流挂收集：每次钉选变化重解析刷新画卷与穿搭签
 *   （有 pin 换成 pin 件、取消 pin 落回当日已定记录，与画廊「今日之裙」签同屏一致）；
 * - 打卡两入口（按钮/环区）共用同一 800ms 前沿闸门（规格 §4.1「按钮=立即记录」）：
 *   首触立即成笔，环推进/数字滚动/夸夸语随即重叠展开（§4.2）；
 *   距上次成笔不足 800ms 的连点合并忽略；
 * - 打卡成功后按当前时段抽一句夸夸语，1.4s 后收起；celebrated=true 时庆祝态撑满 2.5s，
 *   同日后续打卡（use case 返回 false）即时回到普通反馈；
 * - 打卡成功分支后以最新连胜结算解锁（幂等）：新解锁浮出轻提示 [NEW_UNLOCK_HOLD_MS] 后收场，
 *   新一轮打卡以本轮结果当场覆盖旧提示，与夸夸语共用 feedbackEpoch 防串场；
 * - 胆大王的回应编排（moodboard §6.2）：init 按当前小时定心情（22 点后安睡）、按连胜披挂配饰；
 *   打卡成功即 HAPPY 一次并抽一句猫语（回应每次成笔，celebrated 与否不论），[CAT_LINE_HOLD_MS] 后
 *   气泡清空、心情按当前小时落回；摸猫（[petCat]）同样抽一句猫语回应；
 *   猫序列（气泡 + 心情）走独立的 catEpoch 防串场——摸猫/新打卡只互踩猫自己，
 *   不殃及夸夸语/庆祝/新解锁的 feedbackEpoch 收场；
 * - 声音三触发点（任务 12，fire-and-forget 绝不抛）：打卡成笔确认即随机一声掉落音
 *   （[DROP_EVENTS] 三档其一）；celebrated=true 时掉落音后追加一段达标旋律；
 *   摸猫一声呼噜。播放与动画解耦——成笔即响，不等夸夸语/庆祝的任何一拍；
 *   是否出声（应用内开关 + 系统静音遵从）由播放器内部裁决，本层不问。
 *
 * 防抖窗与提示停留时长由 [logDebounceMs]/[newUnlockHoldMs]/[catLineHoldMs] 注入（生产缺省、测试缩窗），
 * 窗口按时钟 [clock] 计量；成笔后反馈序列不取消，仅以 epoch 防串场。
 */
@HiltViewModel
class HomeViewModel(
    private val clock: AppClock,
    observeHome: ObserveHomeUseCase,
    private val logWater: LogWaterUseCase,
    private val copies: CopyLibraryRepository,
    private val unlockOutfits: UnlockOutfitsUseCase,
    private val resolveDailyOutfit: ResolveDailyOutfitUseCase,
    private val streakOf: GetStreakUseCase,
    private val sound: AwakeSoundPlayer,
    private val logDebounceMs: Long = LOG_DEBOUNCE_MS,
    private val newUnlockHoldMs: Long = NEW_UNLOCK_HOLD_MS,
    private val catLineHoldMs: Long = CAT_LINE_HOLD_MS,
) : ViewModel() {
    /** Dagger 注入入口：生产以缺省时长委托主构造器（JSR-330 不识别 Kotlin 缺省参数）。 */
    @Inject
    constructor(
        clock: AppClock,
        observeHome: ObserveHomeUseCase,
        logWater: LogWaterUseCase,
        copies: CopyLibraryRepository,
        unlockOutfits: UnlockOutfitsUseCase,
        resolveDailyOutfit: ResolveDailyOutfitUseCase,
        streakOf: GetStreakUseCase,
        sound: AwakeSoundPlayer,
    ) : this(
        clock,
        observeHome,
        logWater,
        copies,
        unlockOutfits,
        resolveDailyOutfit,
        streakOf,
        sound,
        LOG_DEBOUNCE_MS,
        NEW_UNLOCK_HOLD_MS,
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
        // 胆大王初态（moodboard §6）：按当前小时定心情（22 点后安睡）——猫常驻首页一角，不缺席。
        _uiState.update { it.copy(catMood = resolveCatMood(justCelebrated = false, nowHour = currentHour())) }
        // 猫配饰随连胜刷新：进首页即结算一次（streak 为挂起用例，在协程里取）。
        viewModelScope.launch {
            val accessories = unlockedCatAccessories(streakOf())
            _uiState.update { it.copy(catAccessories = accessories) }
        }
        // 顶部问候语（设计 §9.2）：进首页即从文案库当前时段组抽一句——
        // 每次新建首页导航条目都会重建 VM，故每次进入都是新的一句（去重池防短期重复）。
        viewModelScope.launch {
            val slot = TimeSlots.slotOfHour(currentHour())
            val greeting = copies.randomFor(slot)
            _uiState.update { it.copy(greeting = greeting) }
        }
        // 今日之裙（moodboard §5.1）：进首页即解析（钉选优先/当日已定/解锁池稳定随机），
        // 灌入画卷层与穿搭签；解析完成前 todayOutfit 保持 null——表现层不画卷不显签，UI 完全无感。
        viewModelScope.launch {
            val outfit = resolveDailyOutfit()
            _uiState.update { it.copy(todayOutfit = outfit) }
        }
        // 画廊 pin 回流（终审修复）：首页 VM 存活期间用户在画廊改钉选不重建本 VM，
        // 首值之后的每次 pin 变化都重解析刷新 todayOutfit，与画廊「今日之裙」签同屏一致；
        // resolve 幂等——有 pin 返回 pin、取消 pin 落回当日已定记录，无矛盾态。
        viewModelScope.launch {
            resolveDailyOutfit.pinnedOutfitId.drop(1).collect {
                val outfit = resolveDailyOutfit()
                _uiState.update { it.copy(todayOutfit = outfit) }
            }
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

        // 打卡成功分支后按最新连胜结算解锁（moodboard §5.2，幂等）；
        // 无新解锁时以 null 覆盖——新轮打卡当场清掉旧提示，不让上一轮提示悬挂。
        val newUnlock = result?.let { unlockOutfits(_uiState.value.streakDays).firstOrNull() }

        val slot = TimeSlots.slotOfHour(currentHour())
        val praise = copies.randomFor(slot)
        _uiState.update {
            it.copy(
                praiseLine = praise,
                // 当日首次达标为 true；其余打卡（含达标后再打）一律回到普通反馈。
                celebrating = result?.celebrated == true,
                newUnlock = newUnlock,
            )
        }

        // 新解锁轻提示的定时收场：与夸夸语同一 feedbackEpoch 防串场——
        // 新一轮打卡后本收场失效（新轮已覆盖该字段），不再回写旧值。
        if (newUnlock != null) {
            viewModelScope.launch {
                delay(newUnlockHoldMs)
                if (feedbackEpoch == epoch) {
                    _uiState.update { it.copy(newUnlock = null) }
                }
            }
        }

        // 打卡成功即推进猫序列（moodboard §6.2）：HAPPY 一次 + 抽一句猫语，回应每次成笔。
        if (result != null) {
            // 声音三触发点之一（任务 12）：成笔确认即随机一声掉落音；当日首次达标再追一段旋律。
            // fire-and-forget，与动画解耦——不等夸夸语/庆祝的任何一拍。
            sound.play(DROP_EVENTS.random())
            if (result.celebrated) sound.play(SoundEvent.GOAL_MELODY)
            playCatResponse(happy = true)
            // 猫配饰随连胜刷新：本次成笔后连胜或增，重结算（幂等）。
            viewModelScope.launch {
                val accessories = unlockedCatAccessories(streakOf())
                _uiState.update { it.copy(catAccessories = accessories) }
            }
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
     * 以独立 [catEpoch] 防串场：摸猫/新打卡只换代猫自己，不殃及夸夸语/庆祝/新解锁的收场。
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
