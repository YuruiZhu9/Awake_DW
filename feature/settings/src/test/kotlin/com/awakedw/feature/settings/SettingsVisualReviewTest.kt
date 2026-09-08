package com.awakedw.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.art.loadAssetBitmap
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsVisualReviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings sheet selects and persists theme without an oversized inline theme grid`() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        runBlocking { listOf(ThemeId.EMERALD, ThemeId.CLERIC).forEach { loadAssetBitmap(context, themeArtworkOf(it).asset) } }
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_CLERIC))
        val vm = SettingsViewModel(prefs, FakeCopyLibraryRepository(), FakeWaterRepository(), FakeClock(0L))
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            val state by vm.uiState.collectAsState()
            AwakeTheme(if (state.settings.themeChoice == ThemeChoice.FIXED_CLERIC) ThemeId.CLERIC else ThemeId.EMERALD) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) { SettingsScreen(vm) }
            }
        }
        rule.onNodeWithText("1600ml").assertIsDisplayed()
        capture(view, "settings-cleric")
        rule.onNodeWithText("主题").performScrollTo().assertIsDisplayed()
        capture(view, "settings-preferences")
        rule.onNodeWithText("晨雾蓝瓷").assertDoesNotExist()
        rule.onNodeWithText("主题").performClick()
        rule.onNodeWithText("选择主题").assertIsDisplayed()
        rule.onNodeWithText("晨雾蓝瓷").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(ThemeChoice.FIXED_EMERALD, prefs.settings.value.themeChoice) }
        rule.onNodeWithText("完成").performClick()
        rule.onNodeWithText("选择主题").assertDoesNotExist()
        rule.onNodeWithText("我的").performScrollTo()
        capture(view, "settings-blue")
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
