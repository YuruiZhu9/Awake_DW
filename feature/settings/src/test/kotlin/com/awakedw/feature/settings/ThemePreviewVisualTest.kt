package com.awakedw.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.art.loadAssetBitmap
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.LolitaBackdrop
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.settings.components.ThemeChoiceChips
import com.awakedw.feature.settings.components.themeIdOf
import com.awakedw.feature.settings.components.themeLabel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemePreviewVisualTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `all fixed theme thumbnails expose their primary artwork`() {
        disableMotion()
        val fixedChoices = ThemeChoice.entries.filter { it != ThemeChoice.FOLLOW_TIME }
        runBlocking {
            fixedChoices.forEach { choice ->
                assertNotNull(loadAssetBitmap(RuntimeEnvironment.getApplication(), themeArtworkOf(themeIdOf(choice)).asset))
            }
        }
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                ThemeChoiceChips(selected = ThemeChoice.FOLLOW_TIME, onSelect = {})
            }
        }
        fixedChoices.forEach { choice ->
            val id = themeIdOf(choice)
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithTag("theme-art-${id.name}", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    private fun disableMotion() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
    }

    @Test
    fun `artwork preview cards stay selectable in each dedicated theme`() {
        // Static preview also covers the reduced-motion path; the breathing backdrop never becomes idle otherwise.
        disableMotion()
        // Use the production decode/cache path, but finish IO before native snapshot assertions.
        runBlocking {
            listOf(ThemeId.THIN_MINT, ThemeId.GOTHIC, ThemeId.CLERIC).forEach { id ->
                assertNotNull(loadAssetBitmap(RuntimeEnvironment.getApplication(), themeArtworkOf(id).asset))
            }
        }
        val theme = mutableStateOf(ThemeId.THIN_MINT)
        val selected = mutableStateOf(ThemeChoice.FOLLOW_TIME)
        lateinit var view: View
        composeRule.setContent {
            view = LocalView.current
            AwakeTheme(theme.value) {
                Box(Modifier.fillMaxSize()) {
                    GradientBackdrop(currentThemeSpec(), Modifier.matchParentSize())
                    LolitaBackdrop(currentThemeSpec(), Modifier.matchParentSize())
                    ThemeChoiceChips(
                        selected.value,
                        { selected.value = it },
                        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    )
                }
            }
        }
        listOf(
            ThemeId.THIN_MINT to ThemeChoice.FIXED_THIN_MINT,
            ThemeId.GOTHIC to ThemeChoice.FIXED_GOTHIC,
            ThemeId.CLERIC to ThemeChoice.FIXED_CLERIC,
        ).forEach { (id, choice) ->
            composeRule.runOnIdle { theme.value = id }
            composeRule.onNodeWithText(themeLabel(choice)).performScrollTo().performClick().assertIsSelected()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
                composeRule.mainClock.advanceTimeBy(32L)
                composeRule.waitForIdle()
                composeRule.onAllNodesWithTag("theme-art-${id.name}", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            composeRule.runOnIdle { view.draw(Canvas(bitmap)) }
            val file =
                File(
                    "build/reports/visual-review/${System.getProperty(
                        "awake.visualVariant",
                        "local",
                    )}/alpha10-choices-${id.name.lowercase()}.png",
                )
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
