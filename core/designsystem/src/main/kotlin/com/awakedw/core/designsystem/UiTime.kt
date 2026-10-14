package com.awakedw.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * UI 层的时间出处（2.7.0 视觉基线）：日期刊头与时段氛围等**界面内**时间读数的唯一入口。
 * 默认读真实时钟，行为与此前直接 `LocalDateTime.now()` 逐位一致；
 * 视觉基线测试经 `CompositionLocalProvider(LocalUiTimeMillis provides 固定值)` 注入固定值，
 * 让截图跨日、跨时段稳定。业务时间不经过这里——ViewModel 依旧用注入的领域时钟。
 */
val LocalUiTimeMillis = compositionLocalOf { System.currentTimeMillis() }

/** 当前界面时刻：UI 组件一律走 [LocalUiTimeMillis]，不再直接读系统时钟。 */
@Composable
fun rememberUiNow(): LocalDateTime {
    val timeMillis = LocalUiTimeMillis.current
    return remember(timeMillis) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
    }
}
