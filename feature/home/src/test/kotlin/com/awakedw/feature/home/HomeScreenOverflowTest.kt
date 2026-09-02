package com.awakedw.feature.home

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import org.junit.Assert.assertNotNull
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
        setContentWithLargeFont()

        val button = hasText(BUTTON_LABEL)
        // 内容列可滚（P1-7）：直接调用列的 ScrollBy 语义动作滚到底——
        // 不用 performScrollTo：它内部等待滚动静默，而首页光袋/粒子是无限动画，
        // 帧钟手动模式下静默永不到来（本类曾因此挂死+OOM）。
        scrollColumnToBottom()
        composeRule.onNode(button).assertIsDisplayed()
        // 「完整落在屏内」：语义节点 bounds 全程落在根边界之内，部分越屏不算可发现。
        val rootHeight = composeRule.onRoot().fetchSemanticsNode().size.height.toFloat()
        val buttonBounds = composeRule.onNode(button).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "记一杯按钮应完整落在屏内（根高 $rootHeight）：$buttonBounds",
            buttonBounds.top >= 0f && buttonBounds.bottom <= rootHeight,
        )
    }

    /**
     * 审查修复守护（猫位几何重定位）：猫盒（CatFigure 整盒可点，pointerInput 命中优先）的
     * bounds-in-root 与「记一杯」按钮、快捷胶囊 chip 的 bounding box 在静止态与滚到底态
     * 都不得相交——bounding-box 级断言，任何一态相交即失败（几何失误时修几何、不放宽断言）。
     */
    @Test
    fun `猫盒与记一杯按钮及快捷胶囊bounding box静止与滚到底两态均不相交`() {
        setContentWithLargeFont()

        val button = hasText(BUTTON_LABEL)
        val smallSip = hasText(SMALL_SIP_PREFIX, substring = true)
        val fullSip = hasText(FULL_SIP_PREFIX, substring = true)
        val cat = composeRule.onNodeWithContentDescription(CAT_DESCRIPTION)

        // 静止态（scroll=0）。
        assertCatClearOfButtons(cat, button, smallSip, fullSip, state = "静止态")
        // 滚到底态：按钮滚进视口、居中簇沉到距底固定带，这是猫盒最贴近按钮带的临界位。
        scrollColumnToBottom()
        assertCatClearOfButtons(cat, button, smallSip, fullSip, state = "滚到底态")
    }

    /** 组装假仓库 + 大字体（fontScale 1.3）环境并挂载首页，随后显式走时放行首帧。 */
    private fun setContentWithLargeFont() {
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
    }

    /**
     * 把内容列滚到底（审查修复）：直接调用 [SemanticsActions.ScrollBy]（verticalScroll 列的
     * 无障碍语义动作）并显式走时刷布局——同步、零隐式等待，规避无限动画下的静默死等。
     */
    private fun scrollColumnToBottom() {
        val scrollBy =
            composeRule
                .onNode(hasScrollAction())
                .fetchSemanticsNode()
                .config[SemanticsActions.ScrollBy]?.action
        assertNotNull("内容列应可垂直滚动（布局审计 P1-7）", scrollBy)
        repeat(3) {
            scrollBy!!(0f, 10_000f)
            advanceClock(FIRST_FRAME_MS)
        }
    }

    /** 断言猫盒 bounds 与按钮/两枚快捷胶囊 bounds 在 [state] 态两两不相交（bounding-box 级）。 */
    private fun assertCatClearOfButtons(
        cat: SemanticsNodeInteraction,
        button: SemanticsMatcher,
        smallSip: SemanticsMatcher,
        fullSip: SemanticsMatcher,
        state: String,
    ) {
        val catBounds = cat.fetchSemanticsNode().boundsInRoot
        listOf(
            BUTTON_LABEL to composeRule.onNode(button).fetchSemanticsNode(),
            SMALL_SIP_PREFIX to composeRule.onAllNodes(smallSip)[0].fetchSemanticsNode(),
            FULL_SIP_PREFIX to composeRule.onAllNodes(fullSip)[0].fetchSemanticsNode(),
        ).forEach { (label, node) ->
            val other = node.boundsInRoot
            assertTrue(
                "$state 猫盒 $catBounds 不得与「$label」$other 相交（整盒可点命中优先，几何必须互斥）",
                !intersects(catBounds, other),
            )
        }
    }

    /** 严格相交判定（开闭区间语义）：边缘相触不算相交，重叠哪怕 1px 即相交。 */
    private fun intersects(
        a: Rect,
        b: Rect,
    ): Boolean = a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    private companion object {
        const val BASE_TIME = 1_760_000_000_000L
        const val FIRST_FRAME_MS = 100L

        /** 大字体档位（P1-7 断言口径）：系统无障碍常见最大档。 */
        const val LARGE_FONT_SCALE = 1.3f

        /** 「记一杯」按钮文案（EMERALD 主题）。 */
        const val BUTTON_LABEL = "干杯一下 💧"

        /** 快捷胶囊文案前缀（毫升数随杯容计算，断言走前缀匹配）。 */
        const val SMALL_SIP_PREFIX = "小口"
        const val FULL_SIP_PREFIX = "满杯"

        /** 猫立绘的无障碍描述（CatFigure 语义锚点）。 */
        const val CAT_DESCRIPTION = "胆大王"
    }
}
