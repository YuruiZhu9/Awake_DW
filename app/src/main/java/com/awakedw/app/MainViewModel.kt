package com.awakedw.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.model.ThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 应用壳级状态：把主题解析流抬为 StateFlow 供组合层换肤。
 *
 * 初始值取默认翡翠绿（规格 §2.1），DataStore 首帧即校正；
 * FOLLOW_TIME 的时段重采样由 [ResolveThemeUseCase] 内部负责。
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        resolveTheme: ResolveThemeUseCase,
    ) : ViewModel() {
        val themeId: StateFlow<ThemeId> =
            resolveTheme()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = ThemeId.EMERALD,
                )

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
