package com.awakedw.feature.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.HomeHorizontalPadding
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.home.components.BadgesRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BadgesRowLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var rootView: View
    private val theme = mutableStateOf(ThemeId.EMERALD)

    @Test
    fun `three facts share the entire available width equally`() {
        showSummary(lastDrink = "10:30", interval = "45 分钟")
        assertEvenColumns(listOf("今日 8 杯", "最近一杯 10:30", "平均间隔 45 分钟"))
    }

    /**
     * 触控下限：两行文字的天然高度只有约 40dp，而「最近一杯」承担长按撤回——
     * 高度不够就会长按按空。列宽由上面的用例守着，这里只量高度。
     */
    @Test
    fun `fact columns keep the minimum touch height`() {
        showSummary(lastDrink = "10:30", interval = "45 分钟")
        val minPx = with(rule.density) { ControlMinHeight.toPx() }
        listOf("今日 8 杯", "最近一杯 10:30", "平均间隔 45 分钟").forEach { description ->
            val height = rule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.height
            assertTrue(
                "「$description」的可点高度 ${height}px 低于下限 ${minPx}px（$ControlMinHeight）",
                height >= minPx,
            )
        }
    }

    @Test
    fun `empty last drink leaves two equal columns instead of unused right space`() {
        showSummary(lastDrink = null, interval = "—")
        assertEvenColumns(listOf("今日 8 杯", "平均间隔 —"))
        rule.onNodeWithContentDescription("最近一杯", substring = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun `narrow screen and double font keep long facts in separate full width columns`() {
        showSummary(lastDrink = "昨天 23:59", interval = "12 小时 30 分钟", fontScale = 2f)
        assertEvenColumns(listOf("今日 8 杯", "最近一杯 昨天 23:59", "平均间隔 12 小时 30 分钟"))
    }

    @Test
    fun `all themes retain equal summary columns and produce review images`() {
        showSummary(lastDrink = "10:30", interval = "45 分钟", fontScale = 1.3f)
        ThemeId.entries.forEach { id ->
            rule.runOnIdle { theme.value = id }
            rule.waitForIdle()
            assertEvenColumns(listOf("今日 8 杯", "最近一杯 10:30", "平均间隔 45 分钟"))
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            rule.runOnIdle { rootView.draw(Canvas(bitmap)) }
            val variant = System.getProperty("awake.visualVariant", "local")
            val output = File("build/reports/visual-review/$variant/v040-summary-${id.name.lowercase()}.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun showSummary(
        lastDrink: String?,
        interval: String,
        fontScale: Float = 1f,
    ) {
        rule.setContent {
            rootView = LocalView.current
            AwakeTheme(theme.value) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = HomeHorizontalPadding)) {
                        BadgesRow(8, interval, lastDrink, Modifier.testTag("summary"))
                    }
                }
            }
        }
    }

    private fun assertEvenColumns(descriptions: List<String>) {
        val panel = rule.onNodeWithTag("summary").fetchSemanticsNode().boundsInRoot
        val facts =
            descriptions.map {
                rule.onNodeWithContentDescription(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
        // All configurations are mdpi: 14dp side insets, 8dp inter-column gaps.
        val expectedWidth = (panel.width - 28f - 8f * (facts.size - 1)) / facts.size
        assertEquals(14f, facts.first().left - panel.left, 1f)
        assertEquals(14f, panel.right - facts.last().right, 1f)
        facts.forEach {
            assertEquals(expectedWidth, it.width, 1f)
            assertEquals(facts.first().top, it.top, 1f)
            assertTrue(it.bottom <= panel.bottom)
        }
        facts.zipWithNext().forEach { (left, right) ->
            assertEquals(8f, right.left - left.right, 1f)
        }
    }
}
