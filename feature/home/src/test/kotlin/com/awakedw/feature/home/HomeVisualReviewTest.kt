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

    @Test
    fun `all themes keep persistent hint cat and recording actions in the first viewport`() {
        lateinit var rootView: View
        val theme = mutableStateOf(ThemeId.THIN_MINT)
        val clock = FakeClock(1_760_000_000_000L)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings())
        val vm =
            HomeViewModel(
                clock,
                ObserveHomeUseCase(water, prefs),
                LogWaterUseCase(water, prefs, clock),
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
            val image = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            composeRule.runOnIdle { rootView.draw(Canvas(image)) }
            val output =
                File("build/reports/visual-review/${System.getProperty("awake.visualVariant", "local")}/alpha12-${id.name.lowercase()}.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.mainClock.advanceTimeBy(1_000L)
    }
}
