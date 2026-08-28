package com.awakedw.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 「我的」页步进器 UI 冒烟（Robolectric compose）：
 * 点击「每日目标量」的 ＋/−，数值即时更新且不影响「一杯容量」——
 * 验证乐观更新（连点即时走步）到 UI 文本的整条接线。
 *
 * 目标量与杯容量两枚步进器各有一枚 ＋/−，树序与声明顺序一致（目标量在前），
 * 故 onFirst 恒为目标量的按钮。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `点目标量步进按钮数值即时更新且一杯容量不受影响`() {
        val viewModel = SettingsViewModel(prefs = FakePrefsRepository(), copies = FakeCopyLibraryRepository())
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.EMERALD) {
                SettingsScreen(viewModel = viewModel)
            }
        }
        composeRule.mainClock.advanceTimeBy(FIRST_FRAME_MS)

        composeRule.onNodeWithText("1600ml").assertIsDisplayed()
        composeRule.onNodeWithText("250ml").assertIsDisplayed()

        // ＋ 一次：1600 → 1650；杯容量保持 250ml。
        composeRule.onAllNodesWithText("＋").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(FIRST_FRAME_MS)
        composeRule.onNodeWithText("1650ml").assertIsDisplayed()
        composeRule.onNodeWithText("250ml").assertIsDisplayed()

        // − 两次连点：1650 → 1550（乐观更新，每点一步不丢步）。
        composeRule.onAllNodesWithText("−").onFirst().performClick()
        composeRule.onAllNodesWithText("−").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(FIRST_FRAME_MS)
        composeRule.onNodeWithText("1550ml").assertIsDisplayed()
    }

    private companion object {
        const val FIRST_FRAME_MS = 100L
    }
}
