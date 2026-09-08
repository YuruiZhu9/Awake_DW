package com.awakedw.core.designsystem

import androidx.compose.ui.graphics.Color
import com.awakedw.core.model.ThemeId

/** Silver for monochrome, cocoa for mint chocolate; existing themes retain champagne detail. */
fun ornamentColor(spec: ThemeSpec): Color =
    when (spec.id) {
        ThemeId.EMERALD, ThemeId.GOTHIC, ThemeId.CLERIC -> spec.laceColor
        ThemeId.THIN_MINT -> spec.buttonBottom
        else -> Color(ThemePalette.GOLD_TRIM)
    }
