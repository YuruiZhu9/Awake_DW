package com.awakedw.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用壳级状态：把主题解析流抬为 StateFlow 供组合层换肤；
 * 并持一次性的 onboardingDone 快照供导航壳启动分支判定。
 *
 * 初始值取默认翡翠绿（规格 §2.1），DataStore 首帧即校正；
 * FOLLOW_TIME 的时段重采样由 [ResolveThemeUseCase] 内部负责。
 *
 * [onboardingDone] 三态：null = 首读窗口（开屏续场覆盖，组合层不据此决策）；
 * false = 先入引导；true = 正常进首页。读一次即定，引导内完成由本地导航收口，
 * 不回写此流（防止导航壳在会话中途反向重开引导）。
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        resolveTheme: ResolveThemeUseCase,
        prefs: UserPreferencesRepository,
    ) : ViewModel() {
        val themeId: StateFlow<ThemeId> =
            resolveTheme()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = ThemeId.EMERALD,
                )

        private val _onboardingDone = MutableStateFlow<Boolean?>(null)

        val onboardingDone: StateFlow<Boolean?> = _onboardingDone.asStateFlow()

        init {
            viewModelScope.launch {
                _onboardingDone.value = prefs.onboardingDone()
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
