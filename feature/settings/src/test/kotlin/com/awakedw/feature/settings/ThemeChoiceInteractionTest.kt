package com.awakedw.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.settings.components.ThemeChoiceChips
import com.awakedw.feature.settings.components.themeLabel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class ThemeChoiceInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `all theme cards remain reachable and expose exclusive radio selection at large font`() {
        val selected = mutableStateOf(ThemeChoice.FOLLOW_TIME)
        composeRule.setContent {
            AwakeTheme(ThemeId.GOTHIC) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        ThemeChoiceChips(selected.value, { selected.value = it })
                    }
                }
            }
        }
        var previous = ThemeChoice.FOLLOW_TIME
        ThemeChoice.entries.forEach { choice ->
            composeRule.onNodeWithText(themeLabel(choice)).performScrollTo().assertIsDisplayed().performClick()
                .assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            if (previous != choice) composeRule.onNodeWithText(themeLabel(previous)).assertIsNotSelected()
            previous = choice
        }
    }
}
