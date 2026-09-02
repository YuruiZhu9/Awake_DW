package com.awakedw.feature.home

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 小屏/大字体溢出（布局审计 P1-7，Robolectric compose）：
 * 360dp 窄屏 + fontScale 1.3 时整列内容超出视口，内容列应可垂直滚动，
 * 「记一杯」按钮通过滚动可达且完整落在屏内（语义节点断言「可发现」）。
 *
 * 大字体模拟走 Compose 层 [LocalDensity] 覆写（fontScale=1.3），不依赖模拟器系统设置；
 * 漂浮粒子的帧循环处理与 [HomeScreenTest] 相同：关闭自动推进、显式走时。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class HomeScreenOverflowTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 显式走时：先放行主线程 Handler 上的延迟任务，再推帧钟渲染新状态。 */
    private fun advanceClock(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.mainClock.advanceTimeBy(ms)
    }

    @Test
    fun `小屏大字体下记一杯按钮滚动可达且不越屏`() {
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
                prefs = prefs,
                unlockOutfits = UnlockOutfitsUseCase(prefs),
                resolveDailyOutfit = ResolveDailyOutfitUseCase(prefs, clock),
                streakOf = GetStreakUseCase(water, prefs),
                sound = FakeSoundPlayer(),
            )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.EMERALD) {
                val current = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = current.density, fontScale = LARGE_FONT_SCALE),
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
        advanceClock(FIRST_FRAME_MS)

        val button = hasText("干杯一下 💧")
        // 内容列可滚（P1-7）：performScrollTo 从按钮向上找最近的可滚祖先（verticalScroll 列）把它滚进视口；
        // 内容恰好放得下、无需滚动时此调用为无害空转。
        composeRule.onNode(button).performScrollTo()
        composeRule.onNode(button).assertIsDisplayed()
        // 「完整落在屏内」：语义节点 bounds 全程落在根边界之内，部分越屏不算可发现。
        val rootHeight = composeRule.onRoot().fetchSemanticsNode().size.height.toFloat()
        val buttonBounds = composeRule.onNode(button).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "记一杯按钮应完整落在屏内（根高 $rootHeight）：$buttonBounds",
            buttonBounds.top >= 0f && buttonBounds.bottom <= rootHeight,
        )
    }

    private companion object {
        const val BASE_TIME = 1_760_000_000_000L
        const val FIRST_FRAME_MS = 100L

        /** 大字体档位（P1-7 断言口径）：系统无障碍常见最大档。 */
        const val LARGE_FONT_SCALE = 1.3f
    }
}
