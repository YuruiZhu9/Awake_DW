package com.awakedw.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.LocalUiTimeMillis
import com.awakedw.core.designsystem.art.loadAssetBitmap
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import com.awakedw.core.testsupport.VisualBaseline
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsVisualReviewTest {
    @get:Rule
    val rule = createComposeRule()

    /** 与首页/统计基线同值的钉定时刻：时段氛围恒落在同一确定点（2.7.0）。 */
    private val pinnedUiTimeMillis = 1_760_000_000_000L

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
        preloadThemeAssets(ThemeId.CLERIC)
        preloadThemeAssets(ThemeId.EMERALD)
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_CLERIC))
        val vm = SettingsViewModel(prefs, FakeCopyLibraryRepository(), FakeWaterRepository(), FakeClock(0L))
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            val state by vm.uiState.collectAsState()
            CompositionLocalProvider(LocalUiTimeMillis provides pinnedUiTimeMillis) {
                AwakeTheme(if (state.settings.themeChoice == ThemeChoice.FIXED_CLERIC) ThemeId.CLERIC else ThemeId.EMERALD) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) { SettingsScreen(vm) }
                }
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
        preloadThemeAssets(ThemeId.EMERALD)
        val prefs = FakePrefsRepository(UserSettings())
        val vm = SettingsViewModel(prefs, FakeCopyLibraryRepository(), FakeWaterRepository(), FakeClock(0L))
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalUiTimeMillis provides pinnedUiTimeMillis) {
                AwakeTheme(ThemeId.EMERALD) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) { SettingsScreen(vm) }
                }
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
        // 设置页也是纸页：滚动到底留一张页脚收口的视觉证据。
        rule.onNodeWithTag("settings-footer").performScrollTo().assertIsDisplayed()
        capture(view, "colophon")
    }

    /**
     * 八主题设置页只读遍历（2.7.0，roadmap P1「八主题截图对照」）：每主题一张入库基线。
     * 素材预载、时间钉定、减少动态全静态；不做交互——交互叙事由上面两个用例专责。
     */
    @Test
    fun `settings page stays readable across all eight themes`() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        lateinit var view: View
        val theme = mutableStateOf(ThemeId.EMERALD)
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalUiTimeMillis provides pinnedUiTimeMillis) {
                AwakeTheme(theme.value) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                        SettingsScreen(
                            SettingsViewModel(
                                FakePrefsRepository(UserSettings(themeChoice = themeIdChoiceOf(theme.value))),
                                FakeCopyLibraryRepository(),
                                FakeWaterRepository(),
                                FakeClock(0L),
                            ),
                        )
                    }
                }
            }
        }
        for (id in ThemeId.entries) {
            // 每轮只预载当前主题素材（2.7.0）：主图 + 中景层就位（MRU 常驻），截图不与异步解码赛跑——
            // 全部预载会互相驱逐（theme-night 18% 漂移），漏预载中景则留下负载相关竞态
            // （Release 变体并行负载下 theme-lavender 9.34% 漂移的根因）。
            preloadThemeAssets(id)
            rule.runOnIdle { theme.value = id }
            rule.waitForIdle()
            rule.onNodeWithText("1600ml").assertIsDisplayed()
            capture(view, "theme-${id.name.lowercase()}")
        }
    }

    /** ThemeId → 固定主题选择的一一映射：与设置页「随时间」之外的固定项一一对应。 */
    private fun themeIdChoiceOf(id: ThemeId): ThemeChoice =
        when (id) {
            ThemeId.EMERALD -> ThemeChoice.FIXED_EMERALD
            ThemeId.STRAWBERRY -> ThemeChoice.FIXED_STRAWBERRY
            ThemeId.CARAMEL -> ThemeChoice.FIXED_CARAMEL
            ThemeId.NIGHT -> ThemeChoice.FIXED_NIGHT
            ThemeId.LAVENDER -> ThemeChoice.FIXED_LAVENDER
            ThemeId.GOTHIC -> ThemeChoice.FIXED_GOTHIC
            ThemeId.CLERIC -> ThemeChoice.FIXED_CLERIC
            ThemeId.THIN_MINT -> ThemeChoice.FIXED_THIN_MINT
        }

    /** 预载单主题的主图与中景层素材（2.7.0）：页面像素的全部异步来源就此关闭。 */
    private fun preloadThemeAssets(id: ThemeId) {
        val artwork = themeArtworkOf(id)
        runBlocking {
            artwork.midgroundAsset?.let { loadAssetBitmap(RuntimeEnvironment.getApplication(), it) }
            loadAssetBitmap(RuntimeEnvironment.getApplication(), artwork.asset)
        }
    }

    /** 截图入库并与视觉基线比对（2.7.0）：意外视觉漂移在构建期被绊住，更新走 `AWAKE_VISUAL_BASELINE=update`。 */
    private fun capture(
        view: View,
        label: String,
    ) {
        VisualBaseline.verify(
            module = "settings",
            label = label,
            settle = {
                rule.waitForIdle()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_000L))
                rule.waitForIdle()
            },
        ) {
            lateinit var bitmap: Bitmap
            rule.runOnIdle {
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
            }
            bitmap
        }
    }
}
