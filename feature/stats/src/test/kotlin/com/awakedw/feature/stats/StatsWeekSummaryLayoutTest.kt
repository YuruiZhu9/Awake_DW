package com.awakedw.feature.stats

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.WeekBar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 近七日摘要（0.9.0）的布局回归：0.9.1 细节抛光轮补上——此前的视觉 review 测试
 * 手搓的 [StatsUiState] 从未携带 week 字段，摘要行的取值与展示完全没有断言。
 *
 * 口径：360dp 窄屏 + fontScale 2.0（无障碍常见大字档），两个事实格经
 * `clearAndSetSemantics` 合并读作「标签 值」，断言走合并后的 contentDescription；
 * 值取能被 [StatsMath.weekTotalLabel] 干净整除的数，避免浮点格式化噪声。
 * [StatsMathTest] 已覆盖格式化本身，这里守的是「state → 屏幕」的接线与大字体下不丢内容。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
class StatsWeekSummaryLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `近七日摘要在窄屏大字体下完整展示`() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        // 合计 7200ml → 「7.2 L」；达标（≥1600）三天 → 「3/7 天」，均为干净值。
        val bars =
            listOf(2000, 0, 1600, 500, 0, 1850, 1250).mapIndexed { i, ml ->
                WeekBar(dayKey = "2026-09-0${i + 2}", totalMl = ml)
            }
        val state =
            StatsUiState(
                badges = StatsBadges(1250, 5, "约 1 小时"),
                bars = bars,
                goalMl = 1600,
                weekTotalMl = bars.sumOf { it.totalMl },
                weekMetDays = bars.count { it.totalMl >= 1600 },
            )
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                val current = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(current.density, fontScale = 2f)) {
                    StatsContent(state)
                }
            }
        }

        rule.onNodeWithContentDescription("近七日合计 7.2 L").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("近七日达标 3/7 天").assertIsDisplayed()
    }

    /** 2.13.0 大字回归：2× 下统计页可滚动到「今日记录」，分节标签不横向越屏。 */
    @Test
    fun `两倍字体下整页可滚动到今日记录且不横向越屏`() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        val state =
            StatsUiState(
                badges = StatsBadges(0, 0, "—"),
                bars = emptyList(),
                goalMl = 1600,
                weekTotalMl = 0,
                weekMetDays = 0,
            )
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                val current = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(current.density, fontScale = 2f)) {
                    StatsContent(state)
                }
            }
        }

        val rootWidth = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
        val lastLabel =
            rule
                .onNodeWithText("今日记录 · 0 次")
                .performScrollTo()
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        assertTrue(
            "「今日记录」分节标签右边界越出 360dp 屏：$lastLabel",
            lastLabel.right <= rootWidth,
        )
        // 顶部事实格在 2× 下仍可读，不因大字丢内容。
        rule.onNodeWithContentDescription("近七日合计 0 ml").performScrollTo().assertIsDisplayed()
    }
}
