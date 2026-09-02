package com.awakedw.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.awakedw.core.model.ThemeId

/** Current visual specification exposed to feature modules. */
val LocalAwakeTheme = staticCompositionLocalOf<ThemeSpec> { EmeraldThemeSpec }

/**
 * Typography for Awake_DW: system sans remains the default for data, while display
 * headings use the platform serif family for a restrained editorial/Lolita tone.
 */
val AwakeTypography: Typography =
    Typography().let { base ->
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = FontFamily.Serif),
            displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Serif),
            displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif),
            headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif),
            headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
            headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
            titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
        )
    }

/** Shared shape language: soft paper surfaces without making every control a pill. */
val AwakeShapes: Shapes =
    Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    )

/**
 * Map the explicit Awake palette into Material components as well.
 * Feature screens still use ThemeSpec for bespoke surfaces, but switches, sheets,
 * dialogs and accessibility defaults now share the same theme instead of Material's blue defaults.
 */
fun materialColorSchemeOf(spec: ThemeSpec): ColorScheme {
    val onPrimary = onPrimarySurface(spec)
    return if (spec.isDark) {
        darkColorScheme(
            primary = spec.primary,
            onPrimary = onPrimary,
            primaryContainer = spec.chipBg,
            onPrimaryContainer = spec.chipText,
            secondary = spec.buttonBottom,
            onSecondary = onPrimary,
            background = spec.backgroundGradient.firstOrNull() ?: Color.Black,
            onBackground = spec.greetingColor,
            surface = spec.chipBg,
            onSurface = spec.chipText,
            surfaceVariant = spec.chipBg,
            onSurfaceVariant = spec.greetingSubColor,
            outline = spec.laceColor,
        )
    } else {
        lightColorScheme(
            primary = spec.primary,
            onPrimary = onPrimary,
            primaryContainer = spec.chipBg,
            onPrimaryContainer = spec.chipText,
            secondary = spec.buttonBottom,
            onSecondary = onPrimary,
            background = spec.backgroundGradient.firstOrNull() ?: Color.White,
            onBackground = spec.greetingColor,
            surface = spec.chipBg,
            onSurface = spec.chipText,
            surfaceVariant = spec.chipBg,
            onSurfaceVariant = spec.greetingSubColor,
            outline = spec.laceColor,
        )
    }
}

/** Apply both the custom visual spec and Material defaults for static previews/tests. */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AwakeTheme(
    themeId: ThemeId,
    content: @Composable () -> Unit,
) {
    val spec = ThemeById.getValue(themeId)
    CompositionLocalProvider(LocalAwakeTheme provides spec) {
        AwakeMaterialTheme(spec = spec, content = content)
    }
}

/** Apply Material defaults using the currently animated [ThemeSpec]. */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AwakeMaterialTheme(
    spec: ThemeSpec = currentThemeSpec(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = materialColorSchemeOf(spec),
        typography = AwakeTypography,
        shapes = AwakeShapes,
        content = content,
    )
}

/** Read the current visual specification. */
@Composable
fun currentThemeSpec(): ThemeSpec = LocalAwakeTheme.current
