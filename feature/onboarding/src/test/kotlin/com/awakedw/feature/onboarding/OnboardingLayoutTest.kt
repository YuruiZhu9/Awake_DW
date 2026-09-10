package com.awakedw.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 首次启动引导页的触控与出口覆盖（本页此前没有任何 compose 测试）。
 *
 * 两条断言各守一个真实缺陷面：
 * 1. 两个按钮的触控高度都不低于 [ControlMinHeight]——裸 `clickable` 不受 M3 最小交互尺寸保护，
 *    「以后再说」曾经只有约 40dp；
 * 2. 「以后再说」必须真的能走通：它是「不想被引导」时唯一的出口，卡住等于把人困在这页。
 *
 * 帧钟必须手动推进：本页有水滴呼吸的无限动画与漂浮粒子，自动推进下 `waitForIdle` 永不返回
 * （本测试首次运行就因此挂死，只能停守护进程）。故这里只走 `advanceTimeBy`，不调 `waitForIdle`。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
class OnboardingLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `两个按钮的触控高度都不低于下限`() {
        showOnboarding()

        assertMinTouchHeight("打开设置")
        assertMinTouchHeight("以后再说")
    }

    @Test
    fun `以后再说能走通并置位完成`() {
        val prefs = FakePrefsRepository()
        var completedCount = 0
        showOnboarding(prefs = prefs, onComplete = { completedCount++ })

        rule.onNodeWithText("以后再说").performClick()
        rule.mainClock.advanceTimeBy(FRAME_MS)

        assertEquals("完成接缝应恰好触发一次", 1, completedCount)
        assertEquals("onboarding_done 应落库一次", 1, prefs.markOnboardingCount)
    }

    private fun showOnboarding(
        prefs: FakePrefsRepository = FakePrefsRepository(),
        onComplete: () -> Unit = {},
    ) {
        val viewModel = OnboardingViewModel(prefs, onComplete)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                OnboardingScreen(viewModel = viewModel)
            }
        }
        rule.mainClock.advanceTimeBy(FRAME_MS)
    }

    /** 触控高度按当前密度折算成像素比较，避免依赖 mdpi 下 1dp==1px 的巧合。 */
    private fun assertMinTouchHeight(label: String) {
        val minPx = with(rule.density) { ControlMinHeight.toPx() }
        val bounds = rule.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(
            "「$label」的可点高度 ${bounds.height}px 低于下限 ${minPx}px（$ControlMinHeight）",
            bounds.height >= minPx,
        )
    }

    private companion object {
        const val FRAME_MS = 100L
    }
}
