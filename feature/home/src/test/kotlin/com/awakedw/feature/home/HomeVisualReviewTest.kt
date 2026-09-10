package com.awakedw.feature.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.domain.DeleteWaterRecordUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

/** Native-rendered review artifacts, not golden images or a substitute for device QA. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeVisualReviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 当前内容视图：截图走真实绘制，需要拿到承载 Compose 的根 View。 */
    private lateinit var rootView: View

    @Test
    fun `all themes keep persistent hint cat and recording actions in the first viewport`() {
        val theme = mutableStateOf(ThemeId.THIN_MINT)
        val clock = FakeClock(1_760_000_000_000L)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings())
        val vm =
            HomeViewModel(
                clock,
                ObserveHomeUseCase(water, prefs),
                LogWaterUseCase(water, prefs, clock),
                DeleteWaterRecordUseCase(water),
                FakeCopyLibraryRepository(),
                FakeSoundPlayer(),
            )
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            rootView = LocalView.current
            AwakeTheme(theme.value) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    HomeScreen(viewModel = vm)
                }
            }
        }
        ThemeId.entries.forEach { id ->
            composeRule.runOnIdle { theme.value = id }
            settle()
            composeRule.onNodeWithContentDescription("胆大王").assertIsDisplayed()
            composeRule.onNodeWithText("点击我试试~").assertIsDisplayed()
            composeRule.onNodeWithText("记一杯").assertIsDisplayed()
            val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
            val button = composeRule.onNodeWithText("记一杯").fetchSemanticsNode().boundsInRoot
            assertTrue(button.bottom <= root.bottom)
            capture("home-empty-${id.name.lowercase()}")
        }
    }

    /**
     * 有记录时的首页：这才是使用者日常看到的状态，也是撤回说明行所在的位置。
     * 空态自检看不到这些，单靠断言又判断不了"那行小字压在不同主题背景上还读得出来吗"，所以八个主题各留一张图。
     *
     * 小屏（360×640 @1.3×）上事实条与说明行本就落在首屏之外，需要滚动才能看到——
     * 那是既有基线允许的，所以这里改用常见机型尺寸（411×891）复核：
     * 说明行必须与它所描述的事实条同屏出现，否则这行提示等于白写。
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp-mdpi")
    fun `populated home keeps revert disclosure visible next to the fact row`() {
        val theme = mutableStateOf(ThemeId.THIN_MINT)
        val clock = FakeClock(1_760_000_000_000L)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings())
        val vm =
            HomeViewModel(
                clock,
                ObserveHomeUseCase(water, prefs),
                LogWaterUseCase(water, prefs, clock),
                DeleteWaterRecordUseCase(water),
                FakeCopyLibraryRepository(),
                FakeSoundPlayer(),
            )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            rootView = LocalView.current
            AwakeTheme(theme.value) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    HomeScreen(viewModel = vm)
                }
            }
        }

        // 铺三杯今日记录：事实条、撤回说明行与进度环都进入有数据状态。
        composeRule.runOnIdle { water.seedToday(60, 30) }
        settle()
        composeRule.onNodeWithText(REVERT_HINT_TEXT).assertIsDisplayed()

        ThemeId.entries.forEach { id ->
            composeRule.runOnIdle { theme.value = id }
            settle()
            composeRule.onNodeWithText(REVERT_HINT_TEXT).assertIsDisplayed()
            capture("home-populated-${id.name.lowercase()}")
        }
    }

    /**
     * 打卡瞬间三条反馈通道同屏的状态：环心确认、环下达标缎带、猫语气泡。
     * 这几个元素都只在反馈存活的 1.4s / 2.0s / 2.5s 内存在，
     * 常规空态自检根本拍不到，只能靠"打卡后立刻截图"来留下证据。
     *
     * 浅色与深色主题各自独立成例：庆祝态**一天只触发一次**（`celebrated_day_key` 拦着），
     * 所以同一测试里连点两个主题，第二轮的当日首次达标条件已经不成立。
     * 每例只点一次杯；目标压到 100ml 让首杯即达标，一张图同时覆盖确认语与缎带。
     */
    @Test
    fun `post log feedback channels render together on light theme`() = renderPostLogFeedback(ThemeId.EMERALD)

    @Test
    fun `post log feedback channels render together on dark theme`() = renderPostLogFeedback(ThemeId.GOTHIC)

    private fun renderPostLogFeedback(themeId: ThemeId) {
        val clock = FakeClock(1_760_000_000_000L)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings(goalMl = 100))
        val vm =
            HomeViewModel(
                clock,
                ObserveHomeUseCase(water, prefs),
                LogWaterUseCase(water, prefs, clock),
                DeleteWaterRecordUseCase(water),
                FakeCopyLibraryRepository(),
                FakeSoundPlayer(),
            )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            rootView = LocalView.current
            AwakeTheme(themeId) {
                HomeScreen(viewModel = vm)
            }
        }
        settle()

        composeRule.runOnIdle { vm.tapLogButton() }
        // 只推进到"首帧已渲染、反馈尚未收场"的窗口内（<1.4s）。
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80))
        composeRule.mainClock.advanceTimeBy(600L)

        // 三条通道同时在场：环心确认、达标缎带、猫语气泡。
        composeRule.onNodeWithText(CELEBRATION_TEXT).assertExists()
        composeRule.onNodeWithContentDescription("胆大王").assertIsDisplayed()
        capture("home-feedback-${themeId.name.lowercase()}")
    }

    private fun capture(label: String) {
        val image = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        composeRule.runOnIdle { rootView.draw(Canvas(image)) }
        write(image, label)
    }

    private fun write(
        image: Bitmap,
        label: String,
    ) {
        val variant = System.getProperty("awake.visualVariant", "local")
        val output = File("build/reports/visual-review/$variant/$label.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.mainClock.advanceTimeBy(1_000L)
    }
}
