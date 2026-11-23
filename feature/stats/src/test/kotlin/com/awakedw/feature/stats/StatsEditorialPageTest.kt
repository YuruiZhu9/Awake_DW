package com.awakedw.feature.stats

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.HomeHorizontalPadding
import com.awakedw.core.designsystem.LocalUiTimeMillis
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * 统计页纸面化（大改版 Phase 2）的结构门禁：
 * 页眉 folio、卷首插图、页脚同时在场；卷首守住版心宽度；页脚作为整页收口可滚到。
 * 像素观感交给视觉基线，真机走查仍是最终闸门。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsEditorialPageTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    @Test
    fun `统计页同时有页眉folio卷首与页脚`() {
        setStats()

        rule.onNodeWithTag("editorial-folio").assertExists()
        rule.onNodeWithTag("artwork-plate").assertExists()
        rule.onNodeWithText("卷首 · 9月").assertExists()
        rule.onNodeWithTag("stats-footer").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `卷首插图收在版心内且高度足够`() {
        setStats()

        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val plate = rule.onNodeWithTag("artwork-plate-image").fetchSemanticsNode().boundsInRoot
        val gutter = with(rule.density) { HomeHorizontalPadding.toPx() }
        val plateHeight = with(rule.density) { 104.dp.toPx() }
        assertTrue("卷首应完整落在版心内：plate=$plate root=$root", plate.left >= gutter - 1f && plate.right <= root.right - gutter + 1f)
        assertTrue("图版应为 104dp 高：$plate", abs(plate.height - plateHeight) <= 1f)
    }

    private fun setStats() {
        val state =
            StatsUiState(
                badges = StatsBadges(1250, 5, "约 1 小时"),
                bars = (2..8).map { WeekBar("2026-09-0$it", if (it == 8) 1250 else it * 250) },
                goalMl = 1600,
                timeline = (1..3).map { WaterRecord(it.toLong(), 250, 1_788_854_400_000L + it * 3_600_000L, "2026-09-08") },
                monthLabel = "9月",
                todayKey = "2026-09-08",
            )
        rule.setContent {
            CompositionLocalProvider(LocalUiTimeMillis provides PINNED_TIME) {
                AwakeTheme(ThemeId.EMERALD) {
                    StatsContent(state)
                }
            }
        }
    }

    private companion object {
        /** 与视觉基线同值的钉定时刻：页眉日期与 folio 确定。 */
        const val PINNED_TIME = 1_760_000_000_000L
    }
}
