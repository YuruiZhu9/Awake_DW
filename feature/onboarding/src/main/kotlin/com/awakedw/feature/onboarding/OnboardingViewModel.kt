package com.awakedw.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 白名单引导一屏状态：completed 置位后导航壳负责离开本页。 */
data class OnboardingUiState(
    val completed: Boolean = false,
)

/**
 * 白名单引导 ViewModel：「去设置」成功回执与「以后再说」殊途同归——
 * 置位 onboarding_done（mark-once：只落一次库），并仅首次触发完成接缝供导航壳返回首页。
 *
 * [onComplete] 是完成接缝（默认空实现），对齐 feature/settings 的 onRemindersChanged 模式：
 * 集成任务可经主构造器接上导航回调；生产默认走 @Inject 次构造器（JSR-330 不识别缺省参数）。
 */
class OnboardingViewModel(
    private val prefs: UserPreferencesRepository,
    private val onComplete: () -> Unit = {},
) : ViewModel() {
    /** Dagger 注入入口：生产以空完成回调委托主构造器。 */
    @Inject
    constructor(
        prefs: UserPreferencesRepository,
    ) : this(
        prefs,
        {},
    )

    private val _uiState = MutableStateFlow(OnboardingUiState())

    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** mark-once 闸门：主线程串行调用，重复 complete 不再落库、不再触发回调。 */
    private var completed = false

    /** 「以后再说」与「去设置」成功的共用出口：置位 onboarding_done 并（仅首次）完成引导。 */
    fun complete() {
        if (completed) return
        completed = true
        _uiState.update { it.copy(completed = true) }
        viewModelScope.launch {
            prefs.markOnboardingDone()
            onComplete()
        }
    }

    /** 主按钮回执：跳转成功才完成引导；失败留在本页，可重试或「以后再说」。 */
    fun onWhitelistJumpResult(success: Boolean) {
        if (success) complete()
    }
}
