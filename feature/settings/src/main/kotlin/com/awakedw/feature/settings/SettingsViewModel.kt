package com.awakedw.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.common.AppClock
import com.awakedw.core.domain.NextReminderCalculator
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** 心意文案单句的最大字数（编辑对话框 TextField 同步限长）。 */
const val COPY_MAX_CHARS = 40

/** 「试一试」发送回显的停留时长。 */
const val TEST_SENT_HOLD_MS = 2_500L

/** 「我的」页一屏状态：设置快照 + 文案库快照 + 引导完成标记 + 提醒透明化状态（§11.3）。 */
data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val library: CopyLibrary = CopyLibrary(morning = emptyList(), day = emptyList(), evening = emptyList()),
    val onboardingDone: Boolean = false,
    /** 提醒状态行文案：下一次时刻 / 已关闭 / 今日已达标 / 今日窗口已过。 */
    val reminderStatusLabel: String = "",
    /** 当前是否有有效排程（状态行圆点取色用）。 */
    val reminderArmed: Boolean = false,
    /** 「试一试」刚发送的短暂回显窗口。 */
    val testReminderSent: Boolean = false,
    /** 音效总开关（任务 12，默认开）：切换即乐观更新 + prefs 落库；系统静音时播放器自行安静。 */
    val soundEnabled: Boolean = true,
)

/**
 * 「我的」页 ViewModel：设置与心意文案库全部**即时生效，无保存键**。
 *
 * - 快照流（设置 / 文案库）单向灌入 [SettingsUiState]；
 * - 所有 setter 走「校验 → 直通仓储」：非法输入静默回落原值（规则见 [SettingsValidation]），
 *   调用方不需要也不允许自行夹紧；
 * - [onRemindersChanged] 是提醒总开关的副作用接缝（默认空实现）——
 *   ReminderScheduler 落地后由集成任务在此接上「开/关即重排或取消全部闹钟」；
 * - 提醒状态行（§11.3）由 [NextReminderCalculator] 纯计算——不经调度器写闹钟，
 *   设置每次变化即重算「下一次/已关闭/达标/过窗」；[onPostTestReminder] 为试发通知接缝。
 */
class SettingsViewModel(
    private val prefs: UserPreferencesRepository,
    private val copies: CopyLibraryRepository,
    private val water: WaterRepository,
    private val clock: AppClock,
    private val onRemindersChanged: (Boolean) -> Unit = {},
    private val onPostTestReminder: () -> Unit = {},
) : ViewModel() {
    /** Dagger 注入入口：生产以空副作用回调委托主构造器（JSR-330 不识别 Kotlin 缺省参数）。 */
    @Inject
    constructor(
        prefs: UserPreferencesRepository,
        copies: CopyLibraryRepository,
        water: WaterRepository,
        clock: AppClock,
    ) : this(
        prefs,
        copies,
        water,
        clock,
        {},
        {},
    )

    private val _uiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.settings.collect { s ->
                _uiState.update { it.copy(settings = s) }
                refreshReminderStatus(s)
            }
        }
        viewModelScope.launch {
            copies.library.collect { library ->
                _uiState.update { it.copy(library = library) }
            }
        }
        viewModelScope.launch {
            val done = prefs.onboardingDone()
            _uiState.update { it.copy(onboardingDone = done) }
        }
        // 音效开关（任务 12）：单独的 DataStore 键，独立收集灌入——重进页面如实回显持久值。
        viewModelScope.launch {
            prefs.soundEnabled.collect { enabled ->
                _uiState.update { it.copy(soundEnabled = enabled) }
            }
        }
    }

    /** 每日目标量；非法（越界或步进外）回落原值。 */
    fun setGoalMl(ml: Int) = persistIf({ SettingsValidation.isValidMl(ml) }) { prefs.setGoalMl(ml) }

    /** 一杯容量；非法回落原值。 */
    fun setCupMl(ml: Int) = persistIf({ SettingsValidation.isValidMl(ml) }) { prefs.setCupMl(ml) }

    /**
     * 步进每日目标量（± [SettingsValidation.ML_STEP] 的调用入口）：
     * 以本地状态即时换算并**乐观更新**后再落库——快速连点时每次都基于最新值，
     * 不受 DataStore 异步写回时延影响，一按一步绝不丢步；非法步进（触界）静默忽略。
     */
    fun stepGoalMl(delta: Int) {
        val next = _uiState.value.settings.goalMl + delta
        if (!SettingsValidation.isValidMl(next)) return
        _uiState.update { it.copy(settings = it.settings.copy(goalMl = next)) }
        viewModelScope.launch { prefs.setGoalMl(next) }
    }

    /** 步进一杯容量：语义同 [stepGoalMl]。 */
    fun stepCupMl(delta: Int) {
        val next = _uiState.value.settings.cupMl + delta
        if (!SettingsValidation.isValidMl(next)) return
        _uiState.update { it.copy(settings = it.settings.copy(cupMl = next)) }
        viewModelScope.launch { prefs.setCupMl(next) }
    }

    /** 清醒时段起止；越界 / 非 15min 粒度 / 间隔不足一律回落原窗。 */
    fun setWindow(
        startMin: Int,
        endMin: Int,
    ) = persistIf({ SettingsValidation.isValidWindow(startMin, endMin) }) { prefs.setWindow(startMin, endMin) }

    /** 提醒间隔；不在候选档集合内回落原值。 */
    fun setIntervalMin(intervalMin: Int) =
        persistIf({ SettingsValidation.isValidInterval(intervalMin) }) { prefs.setIntervalMin(intervalMin) }

    /** 提醒总开关；持久化后经 [onRemindersChanged] 接缝通知调度侧（生产当前为空实现）。 */
    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setRemindersEnabled(enabled)
            onRemindersChanged(enabled)
        }
    }

    /** 外观主题选择。 */
    fun setThemeChoice(choice: ThemeChoice) {
        viewModelScope.launch { prefs.setThemeChoice(choice) }
    }

    /**
     * 音效总开关（任务 12）：以本地状态**乐观更新**后再落库（语义同 [stepGoalMl]）——
     * 开关即响不含糊，不受 DataStore 异步写回时延影响；是否真出声由播放器内部裁决
     * （应用内开关 + 系统静音遵从），本层只管写偏好。
     */
    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
        viewModelScope.launch { prefs.setSoundEnabled(enabled) }
    }

    /** 编辑 [slot] 组内第 [index] 句；空文本或超 [COPY_MAX_CHARS] 字一律不落库。 */
    fun upsertCopy(
        slot: TimeSlot,
        index: Int,
        rawText: String,
    ) {
        val text = rawText.trim()
        if (!isValidCopyText(text)) return
        viewModelScope.launch { copies.upsert(slot, index, text) }
    }

    /** 新增一句：按追加语义写到 [slot] 组尾；空文本或超长不落库。 */
    fun addCopy(
        slot: TimeSlot,
        rawText: String,
    ) {
        val text = rawText.trim()
        if (!isValidCopyText(text)) return
        viewModelScope.launch { copies.upsert(slot, _uiState.value.library.groupOf(slot).size, text) }
    }

    /** 删除 [slot] 组内第 [index] 句（越界由仓储静默忽略）。 */
    fun deleteCopy(
        slot: TimeSlot,
        index: Int,
    ) {
        viewModelScope.launch { copies.delete(slot, index) }
    }

    /** 整库恢复出厂默认 30 句。 */
    fun resetCopyLibrary() {
        viewModelScope.launch { copies.resetToDefaults() }
    }

    /** 「试一试」（§11.4）：立即发一条真实样子的提醒通知；发送后短暂回显确认。 */
    fun testReminder() {
        onPostTestReminder()
        _uiState.update { it.copy(testReminderSent = true) }
        viewModelScope.launch {
            delay(TEST_SENT_HOLD_MS)
            _uiState.update { it.copy(testReminderSent = false) }
        }
    }

    /** 提醒状态行重算（§11.3）：与调度器同一纯函数，不写闹钟。 */
    private suspend fun refreshReminderStatus(settings: UserSettings) {
        if (!settings.remindersEnabled) {
            _uiState.update { it.copy(reminderStatusLabel = "提醒已关闭 · 到点不会打扰", reminderArmed = false) }
            return
        }
        val achieved = water.todayStats().totalMl >= settings.goalMl
        val fire = NextReminderCalculator.nextFire(settings, clock, achieved)
        val label =
            when {
                fire != null -> "下一次 · 今天 " + TIME_OF_DAY.format(Instant.ofEpochMilli(fire).atZone(clock.zone()))
                achieved -> "今日已达标 · 明天继续"
                else -> "今日窗口已过 · 明天继续"
            }
        _uiState.update { it.copy(reminderStatusLabel = label, reminderArmed = fire != null) }
    }

    private fun isValidCopyText(text: String): Boolean = text.isNotEmpty() && text.length <= COPY_MAX_CHARS

    private companion object {
        val TIME_OF_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    private inline fun persistIf(
        isValid: () -> Boolean,
        crossinline persist: suspend () -> Unit,
    ) {
        if (!isValid()) return
        viewModelScope.launch { persist() }
    }
}
