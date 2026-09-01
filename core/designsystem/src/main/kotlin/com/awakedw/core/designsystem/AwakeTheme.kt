package com.awakedw.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.awakedw.core.model.ThemeId

/** 当前主题规格的 CompositionLocal，未显式提供时回落到默认清晨天水碧。 */
val LocalAwakeTheme = staticCompositionLocalOf<ThemeSpec> { EmeraldThemeSpec }

/** 按 [ThemeId] 注入对应主题规格，子树内经 [currentThemeSpec] 读取。 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AwakeTheme(
    themeId: ThemeId,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAwakeTheme provides ThemeById.getValue(themeId)) {
        content()
    }
}

/** 读取当前生效的主题规格。 */
@Composable
fun currentThemeSpec(): ThemeSpec = LocalAwakeTheme.current
