package com.awakedw.feature.home

import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.domain.DeleteWaterRecordUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 纸面化页框（大改版 Phase 1，Robolectric）：
 * 页眉 folio、页脚收口、撕票触控高度、窗环尺寸不变，以及 360dp × 520dp
 * 大字体下页脚仍完整落在屏内。像素观感仍走真机验收。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class EditorialPageChromeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun advanceClock(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.mainClock.advanceTimeBy(ms)
    }

    @After
    fun restoreSystemAnimations() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    /**
     * 减少动态回归（2026-11-17）：FadeUpOnce 的无动画路径曾直接忽略 modifier，
     * 页脚会掉到页面左上角；光环的 fillMaxSize 也曾把权重列挤成 0 宽。
     */
    @Test
    fun `减少动态下页脚仍收在纸底且英雄数字可测`() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        setHome()

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val footer = composeRule.onNodeWithTag("home-footer").fetchSemanticsNode().boundsInRoot
        val number = composeRule.onNodeWithTag("home-hero-number").fetchSemanticsNode().boundsInRoot
        val bottomTolerance = with(composeRule.density) { 40.dp.toPx() }
        assertTrue(
            "页脚应常驻纸底：footer=$footer root=$root",
            footer.bottom <= root.bottom && footer.bottom >= root.bottom - bottomTolerance,
        )
        assertTrue("英雄数字不应被环容器挤成 0 宽：$number", number.width > 0f)
    }

    @Test
    fun `页眉有folio且页末有纸页页脚`() {
        setHome()

        composeRule.onNodeWithTag("home-folio").assertExists()
        composeRule.onNodeWithTag("home-footer").assertExists()
    }

    @Test
    @Config(qualifiers = "w360dp-h520dp")
    fun `中小屏大字体下页脚仍完整落在屏内`() {
        setHome(fontScale = 1.3f, seedToday = true)

        val rootHeight = composeRule.onRoot().fetchSemanticsNode().size.height.toFloat()
        val footer = composeRule.onNodeWithTag("home-footer").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "页脚应作为整页收口完整落在屏内（根高 $rootHeight）：$footer",
            footer.bottom <= rootHeight,
        )
    }

    @Test
    fun `两枚撕票保持48dp触控高度`() {
        setHome()

        val minPx = with(composeRule.density) { 48.dp.toPx() }
        listOf("小口", "一杯半").forEach { label ->
            val node =
                composeRule
                    .onNode(hasText(label, substring = true))
                    .fetchSemanticsNode()
            assertTrue(
                "「$label」撕票应 ≥48dp 高：${node.boundsInRoot}",
                node.boundsInRoot.height >= minPx,
            )
        }
    }

    @Test
    fun `英雄数字在环左侧且拥有非零宽度`() {
        setHome()

        val number = composeRule.onNodeWithTag("home-hero-number").fetchSemanticsNode().boundsInRoot
        val ring = composeRule.onNodeWithTag("home-ring-dial").fetchSemanticsNode().boundsInRoot
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("英雄数字应有非零宽度：$number", number.width > 0f)
        assertTrue("英雄数字应完整落在屏内：number=$number root=$root", number.left >= 0f && number.right <= root.right)
        assertTrue("英雄数字应在环左侧：number=$number ring=$ring", number.right <= ring.left)
    }

    @Test
    fun `窗环不改变首页进度环尺寸`() {
        setHome()

        val ring = composeRule.onNodeWithTag("home-ring-dial").fetchSemanticsNode().boundsInRoot
        val expectedPx = with(composeRule.density) { HOME_RING_DIAMETER.toPx() }
        assertEquals("窗环仍应是 132dp 的正方形：$ring", expectedPx, ring.width, 1f)
        assertEquals(expectedPx, ring.height, 1f)
    }

    private fun setHome(
        fontScale: Float = 1f,
        seedToday: Boolean = false,
    ) {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
        val viewModel =
            HomeViewModel(
                clock = clock,
                observeHome = ObserveHomeUseCase(water, prefs),
                logWater = LogWaterUseCase(water, prefs, clock),
                deleteWater = DeleteWaterRecordUseCase(water),
                copies = FakeCopyLibraryRepository(),
                sound = FakeSoundPlayer(),
            )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.EMERALD) {
                val current = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = current.density, fontScale = fontScale),
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
        advanceClock(FIRST_FRAME_MS)
        if (seedToday) {
            composeRule.runOnIdle { water.seedToday(60, 30) }
            advanceClock(FIRST_FRAME_MS)
        }
        advanceClock(SETTLE_MS)
    }

    private companion object {
        const val BASE_TIME = 1_760_000_000_000L
        const val FIRST_FRAME_MS = 100L
        const val SETTLE_MS = 700L
    }
}
