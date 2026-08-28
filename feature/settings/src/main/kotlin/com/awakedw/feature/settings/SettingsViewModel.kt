package com.awakedw.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 心意文案单句的最大字数（编辑对话框 TextField 同步限长）。 */
const val COPY_MAX_CHARS = 40

/** 「我的」页一屏状态：设置快照 + 文案库快照 + 引导完成标记。 */
data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val library: CopyLibrary = CopyLibrary(morning = emptyList(), day = emptyList(), evening = emptyList()),
    val onboardingDone: Boolean = false,
)

/**
 * 「我的」页 ViewModel：设置与心意文案库全部**即时生效，无保存键**。
 *
 * - 快照流（设置 / 文案库）单向灌入 [SettingsUiState]；
 * - 所有 setter 走「校验 → 直通仓储」：非法输入静默回落原值（规则见 [SettingsValidation]），
 *   调用方不需要也不允许自行夹紧；
 * - [onRemindersChanged] 是提醒总开关的副作用接缝（默认空实现）——
 *   ReminderScheduler 落地后由集成任务在此接上「开/关即重排或取消全部闹钟」。
 */
class SettingsViewModel(
    private val prefs: UserPreferencesRepository,
    private val copies: CopyLibraryRepository,
    private val onRemindersChanged: (Boolean) -> Unit = {},
) : ViewModel() {
    /** Dagger 注入入口：生产以空副作用回调委托主构造器（JSR-330 不识别 Kotlin 缺省参数）。 */
    @Inject
    constructor(
        prefs: UserPreferencesRepository,
        copies: CopyLibraryRepository,
    ) : this(
        prefs,
        copies,
        {},
    )

    private val _uiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.settings.collect { s ->
                _uiState.update { it.copy(settings = s) }
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
    }

    /** 每日目标量；非法（越界或步进外）回落原值。 */
    fun setGoalMl(ml: Int) = persistIf({ SettingsValidation.isValidMl(ml) }) { prefs.setGoalMl(ml) }

    /** 一杯容量；非法回落原值。 */
    fun setCupMl(ml: Int) = persistIf({ SettingsValidation.isValidMl(ml) }) { prefs.setCupMl(ml) }

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

    private fun isValidCopyText(text: String): Boolean = text.isNotEmpty() && text.length <= COPY_MAX_CHARS

    private inline fun persistIf(
        isValid: () -> Boolean,
        crossinline persist: suspend () -> Unit,
    ) {
        if (!isValid()) return
        viewModelScope.launch { persist() }
    }
}
