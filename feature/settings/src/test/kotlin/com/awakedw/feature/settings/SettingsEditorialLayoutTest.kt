package com.awakedw.feature.settings

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 设置页编辑部化（2.10.0）的版面自检：
 * 四个分区走同一套共享微标签；分区内去盒后，两个行级动作仍守住 [ControlMinHeight]。
 * 视觉观感交给八主题基线，这里只钉住「结构存在」和「够不够点」。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class SettingsEditorialLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `四个分区都有微标签且行级动作守住触控下限`() {
        val viewModel =
            SettingsViewModel(
                prefs = FakePrefsRepository(),
                copies = FakeCopyLibraryRepository(),
                water = FakeWaterRepository(),
                clock = FakeClock(0L),
            )
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                SettingsScreen(viewModel = viewModel)
            }
        }
        rule.waitForIdle()

        listOf("饮水目标", "提醒", "外观与声音", "心意文案库", "提醒稳定性").forEach { label ->
            rule.onAllNodesWithText(label).onFirst().performScrollTo().assertIsDisplayed()
        }

        assertMinTouchHeight("settings-test-reminder", "测试提醒")
        assertMinTouchHeight("settings-guide-entry", "让提醒更稳定")
    }

    /**
     * 2.13.0 大字回归：360dp + fontScale 2.0 下整页仍可滚动到最后一个行级动作，
     * 控件不横向越屏、可点高度不因换行被压扁。设置页允许纵向滚动，但不允许截断或覆盖。
     */
    @Test
    @Config(qualifiers = "w360dp-h891dp")
    fun `两倍字体下整页可滚动到底且行级控件不越屏`() {
        val viewModel =
            SettingsViewModel(
                prefs = FakePrefsRepository(),
                copies = FakeCopyLibraryRepository(),
                water = FakeWaterRepository(),
                clock = FakeClock(0L),
            )
        rule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(current.density, fontScale = 2f)) {
                AwakeTheme(ThemeId.EMERALD) {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
        rule.waitForIdle()

        val rootWidth = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
        val minPx = with(rule.density) { ControlMinHeight.toPx() }
        listOf("settings-test-reminder" to "测试提醒", "settings-guide-entry" to "让提醒更稳定").forEach { (tag, label) ->
            val bounds =
                rule
                    .onNodeWithTag(tag)
                    .performScrollTo()
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot
            assertTrue(
                "「$label」右边界越出 360dp 屏：$bounds",
                bounds.right <= rootWidth,
            )
            assertTrue(
                "「$label」可点高度 ${bounds.height}px 低于下限 ${minPx}px",
                bounds.height >= minPx,
            )
        }
        // 最后一个分区动作已在视口内：滚动到底不会把「提醒稳定性」截在底栏之后。
        rule.onNodeWithTag("settings-guide-entry").assertIsDisplayed()
    }

    /** 触控高度按当前密度折算成像素比较，避免依赖 mdpi 下 1dp==1px 的巧合。 */
    private fun assertMinTouchHeight(
        tag: String,
        label: String,
    ) {
        val minPx = with(rule.density) { ControlMinHeight.toPx() }
        val bounds =
            rule
                .onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        assertTrue(
            "「$label」的可点高度 ${bounds.height}px 低于下限 ${minPx}px（$ControlMinHeight）",
            bounds.height >= minPx,
        )
    }
}
