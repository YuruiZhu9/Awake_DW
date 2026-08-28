package com.awakedw.app

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 冷启动全链路冒烟（Robolectric）：真实 [MainActivity]（@AndroidEntryPoint +
 * installSplashScreen）→ [AwakeApp] 首帧 → [SplashMorph] 续场自然放行 →
 * 导航壳启动分支 → 全新安装落引导页。任何启动期异常（DI 缺绑定、主题/资源、
 * 首组合崩溃）都会在本测试抛出真堆栈——对齐真机开屏闪退的排查路径。
 *
 * 开屏粒子/涟漪是帧循环：与 HomeScreenTest 同法关闭自动走时，显式推过
 * 开屏时序（~1.2s + 交棒）后放行主线程挂起的协程再断言。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = AwakeApplication::class, qualifiers = "w411dp-h891dp")
class MainActivityColdBootTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `冷启动走完开屏续场全新安装落引导页`() {
        composeRule.mainClock.autoAdvance = false
        // 开屏总时序 ~1.2s（水滴 450 + 涟漪 500 + 形序 250）+ 交棒半拍。
        composeRule.mainClock.advanceTimeBy(SPLASH_TOTAL_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SPLASH_TOTAL_MS))
        // 交棒后导航壳挂载：DataStore 首读（onboardingDone 三态）+ 引导页首帧。
        composeRule.mainClock.advanceTimeBy(HOME_FIRST_FRAME_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))

        composeRule.onNodeWithText("为了让每一次温柔准时抵达").assertIsDisplayed()
        composeRule.onNodeWithText("去设置 ♡").assertIsDisplayed()
    }

    @Test
    fun `引导页以后再说后进入真首页`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(SPLASH_TOTAL_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SPLASH_TOTAL_MS))
        composeRule.mainClock.advanceTimeBy(HOME_FIRST_FRAME_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))

        // 跳过引导：complete() 落 DataStore → onComplete 接缝导航首页 → 真首页挂载。
        composeRule.onNodeWithText("以后再说").performClick()
        composeRule.mainClock.advanceTimeBy(HOME_FIRST_FRAME_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))

        composeRule.onNodeWithText("干杯一下 💧").assertIsDisplayed()
    }

    private companion object {
        const val SPLASH_TOTAL_MS = 1_600L
        const val HOME_FIRST_FRAME_MS = 400L
        const val SETTLE_MS = 3_000L
    }
}
