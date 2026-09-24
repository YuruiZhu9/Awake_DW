package com.awakedw.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max

/**
 * The quiet reading layer between artwork and content.
 *
 * It is intentionally a field rather than a panel: the center receives a very soft,
 * theme-aware paper/ink wash and the edges remain transparent so the illustration can
 * keep breathing. It carries no semantics, state, randomness, or business meaning.
 */
private const val LIGHT_READING_FIELD_ALPHA = 0.12f
private const val DARK_READING_FIELD_ALPHA = 0.16f

/** Maximum opacity budget for the reading field; kept low enough to avoid a visible card. */
internal fun readingFieldAlpha(spec: ThemeSpec): Float = if (spec.isDark) DARK_READING_FIELD_ALPHA else LIGHT_READING_FIELD_ALPHA

/** The reading field uses paper in light themes and ink in dark themes. */
internal fun readingFieldColor(spec: ThemeSpec): Color = if (spec.isDark) Color.Black else Color.White

/**
 * A static, softly feathered contrast field used by all content pages.
 *
 * Placement is deliberately after the artwork layers and before particles/content. This
 * makes the central reading column quieter without dimming the small edge particles or
 * introducing another framed surface.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun ReadingField(
    spec: ThemeSpec = currentThemeSpec(),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .drawWithCache {
                    val fieldColor = readingFieldColor(spec)
                    val fieldAlpha = readingFieldAlpha(spec)
                    val center = Offset(size.width * 0.5f, size.height * 0.44f)
                    val radius = max(size.width * 0.78f, size.height * 0.62f)
                    val verticalWash =
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0.00f to Color.Transparent,
                                    0.16f to fieldColor.copy(alpha = fieldAlpha * 0.22f),
                                    0.36f to fieldColor.copy(alpha = fieldAlpha * 0.72f),
                                    0.72f to fieldColor.copy(alpha = fieldAlpha * 0.70f),
                                    0.92f to fieldColor.copy(alpha = fieldAlpha * 0.24f),
                                    1.00f to Color.Transparent,
                                ),
                        )
                    val centralWash =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0.00f to fieldColor.copy(alpha = fieldAlpha * 0.50f),
                                    0.42f to fieldColor.copy(alpha = fieldAlpha * 0.30f),
                                    1.00f to Color.Transparent,
                                ),
                            center = center,
                            radius = radius,
                        )
                    onDrawBehind {
                        drawRect(verticalWash)
                        drawRect(centralWash)
                    }
                },
    )
}
