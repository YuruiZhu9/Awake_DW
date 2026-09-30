package com.awakedw.feature.onboarding

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.model.ThemeChoice
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
import org.robolectric.Shadows.shadowOf
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
// 主题步（2.4.0）含首页缩样与八张色卡，内容高于常规竖屏（引导内滚动，属预期）——
// 测试视口取 h980dp 保证主路径元素全部可见，触控与点击断言无须依赖滚动。
@Config(qualifiers = "w360dp-h980dp-mdpi")
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
    fun `主题步展示色卡与缩样且选择即时落库`() {
        val prefs = FakePrefsRepository()
        showOnboarding(prefs = prefs)

        rule.onNodeWithText("先挑一个顺眼的主题").assertIsDisplayed()
        rule.onNodeWithText("晨雾蓝瓷").assertIsDisplayed()
        rule.onNodeWithText("记一杯").assertIsDisplayed()

        rule.onNodeWithText("黑色哥特").performClick()
        advanceClock()
        assertEquals(listOf(ThemeChoice.FIXED_GOTHIC), prefs.themeChoiceWrites)

        rule.onNodeWithText("就这个，继续").performClick()
        advanceClock()
        rule.onNodeWithText("打开设置").assertIsDisplayed()
    }

    @Test
    fun `两个按钮的触控高度都不低于下限`() {
        showOnboarding()

        // 主题步主按钮（「就这个，继续」）与白名单步两枚按钮都不得低于触控下限。
        assertMinTouchHeight("就这个，继续")
        rule.onNodeWithText("就这个，继续").performClick()
        advanceClock()
        assertMinTouchHeight("打开设置")
        assertMinTouchHeight("以后再说")
    }

    @Test
    fun `以后再说能走通并置位完成`() {
        val prefs = FakePrefsRepository()
        var completedCount = 0
        showOnboarding(prefs = prefs, onComplete = { completedCount++ })

        rule.onNodeWithText("就这个，继续").performClick()
        advanceClock()
        rule.onNodeWithText("以后再说").performClick()
        advanceClock()

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
        advanceClock()
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

    /** 双泵走时（对齐 HomeScreenOverflowTest）：先排空主线程 Handler（资源图解码回调），再推帧钟。 */
    private fun advanceClock(ms: Long = FRAME_MS) {
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(ms))
        rule.mainClock.advanceTimeBy(ms)
    }

    private companion object {
        const val FRAME_MS = 100L
    }
}
