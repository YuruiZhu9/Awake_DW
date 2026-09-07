package com.awakedw.feature.home

import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.CatMood
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.home.components.LogButton
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
        assertTrue(text.right <= cat.left)
        assertTrue(text.bottom <= button.top)
        assertTrue(cat.bottom <= button.top)
    }
}
