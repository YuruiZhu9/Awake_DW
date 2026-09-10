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
import com.awakedw.feature.settings.copyeditor.COPY_ITEM_HINT
import com.awakedw.feature.settings.copyeditor.COPY_SCOPE_FOOTER
import com.awakedw.feature.settings.copyeditor.COPY_SCOPE_SUBTITLE
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
        capture(view, "cleric")
        rule.onNodeWithText("主题").performScrollTo().assertIsDisplayed()
        capture(view, "preferences")
        rule.onNodeWithText("晨雾蓝瓷").assertDoesNotExist()
        rule.onNodeWithText("主题").performClick()
        rule.onNodeWithText("选择主题").assertIsDisplayed()
        rule.onNodeWithText("晨雾蓝瓷").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(ThemeChoice.FIXED_EMERALD, prefs.settings.value.themeChoice) }
        rule.onNodeWithText("完成").performClick()
        rule.onNodeWithText("选择主题").assertDoesNotExist()
        rule.onNodeWithText("我的").performScrollTo()
        capture(view, "blue")
    }

    /**
     * 心意文案库分区：本轮改动了它的分区说明、分组内操作说明与页脚职责澄清，
     * 但它在设置页折叠区之后，之前的自检从没滚到过这里，等于改动无人核对。
     * 这里滚到分区、展开早组，把说明行与页脚一并拍下来。
     */
    @Test
    fun `copy library section states its scope and discloses the delete gesture`() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val prefs = FakePrefsRepository(UserSettings())
        val vm = SettingsViewModel(prefs, FakeCopyLibraryRepository(), FakeWaterRepository(), FakeClock(0L))
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            AwakeTheme(ThemeId.EMERALD) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) { SettingsScreen(vm) }
            }
        }

        rule.onNodeWithText("心意文案库").performScrollTo().assertIsDisplayed()
        // 说明行紧跟标题，滚到标题时它可能刚好在折叠线外，所以自己滚一次。
        rule.onNodeWithText(COPY_SCOPE_SUBTITLE).performScrollTo().assertIsDisplayed()
        // 分组头是 merged 语义节点：点它即展开该组。
        // 必须先 performScrollTo——点击不会自动滚动，节点在视口外时点击落不到控件上。
        rule.onNodeWithText("早上").performScrollTo().performClick()
        // 展开后把说明行滚进视口再抓图，否则拍到的仍是被折叠线切掉的组头。
        rule.onNodeWithText(COPY_ITEM_HINT).performScrollTo().assertIsDisplayed()
        capture(view, "copy-library")

        // 页脚澄清文案库与打卡确认/猫语的分工。
        rule.onNodeWithText(COPY_SCOPE_FOOTER).performScrollTo().assertIsDisplayed()
    }

    private fun capture(
        view: View,
        label: String,
    ) {
        rule.waitForIdle()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnIdle { view.draw(Canvas(bitmap)) }
        val variant = System.getProperty("awake.visualVariant", "local")
        val file = File("build/reports/visual-review/$variant/settings-$label.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
