package com.awakedw.feature.home

import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.CatMood
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.home.components.LogButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class CatRailLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `long response expands its row without covering cat or following action`() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val line = "喝过水了，休息一下。我把水杯放在手边，忙完这一小段也记得喝水。".repeat(2)
        composeRule.setContent {
            AwakeTheme(ThemeId.THIN_MINT) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                    Column {
                        CatRail(CatMood.IDLE, line, {}, Modifier.fillMaxWidth())
                        LogButton(onTap = {}, cupMl = 250)
                    }
                }
            }
        }
        val text = composeRule.onNodeWithText(line).fetchSemanticsNode().boundsInRoot
        val cat = composeRule.onNodeWithContentDescription("胆大王").fetchSemanticsNode().boundsInRoot
        val button = composeRule.onNodeWithText("记一杯").fetchSemanticsNode().boundsInRoot
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(line).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(TextAlign.Start, layouts.single().layoutInput.style.textAlign)
        for (index in 0 until layouts.single().lineCount) assertEquals(0f, layouts.single().getLineLeft(index), 0.01f)
        assertTrue(text.right <= cat.left)
        assertTrue(text.bottom <= button.top)
        assertTrue(cat.bottom <= button.top)
    }

    @Test
    fun `idle mascot rail fills the response side without adding a fake message`() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        var taps = 0
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                CatRail(CatMood.IDLE, null, { taps++ }, Modifier.fillMaxWidth())
            }
        }
        composeRule.onNodeWithText("点击我试试~").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("胆大王").assertIsDisplayed()
        composeRule.onAllNodesWithText("等待回应").assertCountEquals(0)
        assertEquals(0, taps)
    }

    @Test
    fun `hint remains visible and clickable during and after cat responses`() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val line = mutableStateOf<String?>(null)
        var taps = 0
        composeRule.setContent {
            AwakeTheme(ThemeId.CLERIC) {
                CatRail(CatMood.IDLE, line.value, {
                    taps++
                    line.value = "喝过水了。"
                }, Modifier.fillMaxWidth())
            }
        }
        composeRule.onNodeWithText("点击我试试~").performClick()
        assertEquals(1, taps)
        composeRule.onNodeWithText("点击我试试~").assertIsDisplayed()
        composeRule.onNodeWithText("点击我试试~").performClick()
        assertEquals(2, taps)
        composeRule.onNodeWithContentDescription("胆大王").performClick()
        assertEquals(3, taps)
        composeRule.runOnIdle { line.value = null }
        composeRule.onNodeWithText("点击我试试~").assertIsDisplayed()
    }
}
