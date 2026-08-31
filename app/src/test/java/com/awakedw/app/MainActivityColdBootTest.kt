package com.awakedw.app

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
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

    @Test
    fun `页签切换后系统返回键在首页直接退出应用`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(SPLASH_TOTAL_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SPLASH_TOTAL_MS))
        composeRule.mainClock.advanceTimeBy(HOME_FIRST_FRAME_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))
        composeRule.onNodeWithText("以后再说").performClick()
        composeRule.mainClock.advanceTimeBy(HOME_FIRST_FRAME_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))
        composeRule.onNodeWithText("干杯一下 💧").assertIsDisplayed()

        // 统计 → 首页一轮页签切换：栈应始终保持在 [首页] 一层
        // （回归：popUpTo 若指向已被弹出的 onboarding startDestination 会变成空操作，
        //  每次切页签都往栈里压重复条目，返回键要按很多下才退得出去）。
        composeRule.onNodeWithText("统计").performClick()
        composeRule.mainClock.advanceTimeBy(HOME_FIRST_FRAME_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))
        // 统计页真实挂载（hiltViewModel 注入 + 今日徽章渲染）。
        composeRule.onAllNodesWithText("今日 0 杯 ☀")[0].assertIsDisplayed()
        composeRule.onNodeWithText("首页").performClick()
        composeRule.mainClock.advanceTimeBy(HOME_FIRST_FRAME_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))

        // 系统返回键：栈里只剩首页一条 → 返回直达 Activity.finish（应用退出）。
        // 断言 isFinishing：返回未被任何 BackHandler 吞掉、Activity 走到退出。
        // （不追 ON_DESTROY——Robolectric 的 ActivityScenario 在普通 looper 空转里
        //   不传播销毁回调，isFinishing 即「返回键退出应用」的可靠证据。）
        var finishingAfterBack = false
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            finishingAfterBack = activity.isFinishing
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))
        assertTrue("首页返回键应直接退出应用", finishingAfterBack)
    }

    private companion object {
        const val SPLASH_TOTAL_MS = 1_600L
        const val HOME_FIRST_FRAME_MS = 400L
        const val SETTLE_MS = 3_000L
    }
}
