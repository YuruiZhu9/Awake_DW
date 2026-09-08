package com.awakedw.feature.stats

import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.art.loadAssetBitmap
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsVisualReviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `populated stats stay readable and each day exposes its real amount`() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        runBlocking { listOf(ThemeId.EMERALD, ThemeId.CLERIC).forEach { loadAssetBitmap(context, themeArtworkOf(it).asset) } }
        lateinit var view: View
        val theme = mutableStateOf(ThemeId.EMERALD)
        val font = mutableStateOf(1f)
        val state =
            StatsUiState(
                StatsBadges(1250, 5, "约 1 小时"),
                (2..8).map { WeekBar("2026-09-0$it", if (it == 8) 1250 else it * 250) },
                1600,
                (1..5).map { WaterRecord(it.toLong(), 250, 1_788_854_400_000L + it * 3_600_000L, "2026-09-08") },
            )
        rule.setContent {
            view = LocalView.current
            AwakeTheme(theme.value) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, font.value)) { StatsContent(state) }
            }
        }
        for (id in listOf(ThemeId.EMERALD, ThemeId.CLERIC)) {
            rule.runOnIdle { theme.value = id }
            rule.onNodeWithText("统计").performScrollTo().assertIsDisplayed()
            capture(view, "stats-${id.name.lowercase()}")
        }
        rule.runOnIdle { font.value = 1.5f }
        rule.onNodeWithContentDescription("2026-09-04，1000ml").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithText("2026-09-04 · 1000ml").assertIsDisplayed()
        capture(view, "stats-large-chart")
        rule.onNodeWithText("今日记录 · 5 次").performScrollTo().assertIsDisplayed()
        capture(view, "stats-large-records")
    }

    @Test
    fun `empty stats show a plain empty state without an imaginary record`() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        rule.setContent { AwakeTheme(ThemeId.CLERIC) { StatsContent(StatsUiState()) } }
        rule.onNodeWithText("今天还没有饮水记录").performScrollTo().assertIsDisplayed()
    }

    private fun capture(
        view: View,
        label: String,
    ) {
        rule.waitForIdle()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnIdle { view.draw(Canvas(bitmap)) }
        val file = File("build/reports/visual-review/${System.getProperty("awake.visualVariant", "local")}/alpha11-$label.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
