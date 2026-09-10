package com.awakedw.feature.settings

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * 心意文案库编辑器的触控下限（§11 触控目标不低于 [ControlMinHeight]）。
 *
 * 编辑器里的控件全是裸 `clickable`／`combinedClickable`——它们不像 M3 的 Button 那样自带
 * 最小交互尺寸，高度完全由内边距决定，所以「看着还行」和「够不够点」是两件事，必须量。
 * 本测试按节点实际 bounds 逐个量：恢复默认、分组头、新增一句、以及句子行本身。
 *
 * 必须把系统动画时长置零：设置页有漂浮粒子，`waitForIdle` 在自动推进下永不返回
 * （首次运行因此报了 60 秒 idle 超时）。这与 `SettingsVisualReviewTest` 的做法一致。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
class CopyEditorTouchTargetTest {
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
    fun `文案库各控件的可点高度都不低于下限`() {
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

        assertMinTouchHeight("恢复默认")
        assertMinTouchHeight("早上")
        // 展开早组：组头是 merged 语义节点，先滚动再点，否则点击落在视口外。
        rule.onNodeWithText("早上").performScrollTo().performClick()
        rule.waitForIdle()

        assertMinTouchHeight("＋ 新增一句")
        assertMinTouchHeight("早安，喝水啦")
    }

    /** 触控高度按当前密度折算成像素比较，避免依赖 mdpi 下 1dp==1px 的巧合。 */
    private fun assertMinTouchHeight(label: String) {
        val minPx = with(rule.density) { ControlMinHeight.toPx() }
        val bounds =
            rule
                .onNodeWithText(label)
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
