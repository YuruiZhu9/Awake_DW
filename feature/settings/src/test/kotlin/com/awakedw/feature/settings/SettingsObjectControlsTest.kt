package com.awakedw.feature.settings

import android.provider.Settings
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 设置页器物控件（大改版 Phase 2）：压印开关保留 Switch 语义并即时落库，
 * 键帽步进与印章票守住 48dp；页眉 folio 与页脚作为页面收口在场。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
class SettingsObjectControlsTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `压印开关保留Switch语义并即时落库`() {
        val prefs = FakePrefsRepository()
        setSettings(prefs)

        val toggles = rule.onAllNodes(isToggleable())
        toggles.assertCountEquals(2)
        toggles
            .onFirst()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()
        rule.waitForIdle()

        toggles.onFirst().assertIsOff()
        assertTrue("首个开关应把「喝水提醒」写入 prefs：${prefs.calls}", prefs.calls.contains("reminders=false"))
    }

    @Test
    fun `键帽与印章票守住48dp触控`() {
        setSettings(FakePrefsRepository())
        rule.waitForIdle()
        val minPx = with(rule.density) { ControlMinHeight.toPx() }

        listOf(
            "＋" to rule.onAllNodesWithText("＋").onFirst(),
            "−" to rule.onAllNodesWithText("−").onFirst(),
            "60" to rule.onNodeWithText("60"),
        ).forEach { (label, node) ->
            val bounds = node.performScrollTo().fetchSemanticsNode().boundsInRoot
            assertTrue("「$label」可点高度 ${bounds.height}px 低于 $minPx：$bounds", bounds.height >= minPx)
        }
    }

    @Test
    fun `设置页有页眉folio与页脚且页脚可滚到`() {
        setSettings(FakePrefsRepository())
        rule.waitForIdle()

        rule.onNodeWithTag("editorial-folio").assertExists()
        rule.onNodeWithTag("settings-footer").performScrollTo().assertIsDisplayed()
        // 页脚应完整落在屏内，不被底部留白挤出。
        val footer = rule.onNodeWithTag("settings-footer").fetchSemanticsNode().boundsInRoot
        assertTrue("页脚高度应大于 0：$footer", footer.height > 0f)
    }

    private fun setSettings(prefs: FakePrefsRepository) {
        val viewModel =
            SettingsViewModel(
                prefs = prefs,
                copies = FakeCopyLibraryRepository(),
                water = FakeWaterRepository(),
                clock = FakeClock(0L),
            )
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
