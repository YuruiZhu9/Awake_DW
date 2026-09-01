package com.awakedw.feature.home

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.domain.GetStreakUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveDailyOutfitUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.domain.UnlockOutfitsUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 首页打卡交互（Robolectric compose）：
 * 点击「记一杯」→ 环心总数立即更新为新一杯（前沿闸门，规格 §4.1「按钮=立即记录」）；
 * 假钟未动的连点落在防抖窗内合并，只记一杯；假钟跨窗后可再成一笔。
 *
 * 首页的漂浮粒子是永不停止的帧循环，故关闭 mainClock 自动推进、由测试显式走时：
 * advanceTimeBy 推进 Compose 帧钟渲染新状态，idleFor 放行主线程上挂着的反馈时延。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 显式走时：先放行主线程 Handler 上的延迟任务，再推帧钟渲染新状态。 */
    private fun advanceClock(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.mainClock.advanceTimeBy(ms)
    }

    @Test
    fun `点击打卡按钮后环心更新且窗口内连点只记一杯`() {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
        val copies = FakeCopyLibraryRepository()
        val viewModel =
            HomeViewModel(
                clock = clock,
                observeHome = ObserveHomeUseCase(water, prefs, ResolveThemeUseCase(prefs, clock)),
                logWater = LogWaterUseCase(water, prefs, clock),
                copies = copies,
                unlockOutfits = UnlockOutfitsUseCase(prefs),
                resolveDailyOutfit = ResolveDailyOutfitUseCase(prefs, clock),
                streakOf = GetStreakUseCase(water, prefs),
                sound = FakeSoundPlayer(),
            )

        var galleryOpens = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.EMERALD) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenGallery = { galleryOpens++ },
                )
            }
        }
        advanceClock(FIRST_FRAME_MS)

        composeRule.onNodeWithText("0ml").assertIsDisplayed()
        composeRule.onNodeWithText("干杯一下 💧").assertIsDisplayed()
        // 今日之裙文字签已移除：今日穿搭信息回归衣橱页呈现，首页不再有常驻穿搭文字。
        composeRule.onNodeWithText("今日之裙 · 素呢初见").assertDoesNotExist()
        // 问候语行右端的蝴蝶结衣橱入口：语义节点上屏，点击回调 onOpenGallery。
        composeRule.onNodeWithContentDescription("衣橱").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("衣橱").performClick()
        assertEquals(1, galleryOpens)

        // 假钟未动的同刻连点：前沿闸门合并，只记一杯。
        composeRule.onNodeWithText("干杯一下 💧").performClick()
        composeRule.onNodeWithText("干杯一下 💧").performClick()
        advanceClock(RENDER_SETTLE_MS)

        composeRule.onNodeWithText("250ml").assertIsDisplayed()
        assertEquals(1, water.addCount)
        // 首杯命中开局件解锁：同位浮出「新裙入柜」轻提示（2.5s 收场，尚未到时）。
        composeRule.onNodeWithText("新裙入柜 ♡ 素呢初见").assertIsDisplayed()

        // 假钟拨过 800ms 防抖窗后再点一杯：正常成笔。
        clock.ms += WINDOW_GAP_MS
        composeRule.onNodeWithText("干杯一下 💧").performClick()
        advanceClock(RENDER_SETTLE_MS)

        composeRule.onNodeWithText("500ml").assertIsDisplayed()
        assertEquals(2, water.addCount)
    }

    private companion object {
        const val FIRST_FRAME_MS = 100L
        const val RENDER_SETTLE_MS = 1_500L
        const val WINDOW_GAP_MS = 2_000L
    }
}
