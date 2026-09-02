package com.awakedw.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec

/** A quiet status chip: translucent paper surface plus a fine lace-colored edge. */
private val CHIP_SHAPE: Shape = RoundedCornerShape(percent = 50)
private const val CHIP_FILL_ALPHA = 0.78f
private const val CHIP_BORDER_ALPHA = 0.62f

/**
 * Shared status chip for home and statistics. It carries information without
 * becoming another solid decorative block in the composition.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun BadgeChip(
    text: String,
    spec: ThemeSpec,
) {
    Surface(
        shape = CHIP_SHAPE,
        color = spec.chipBg.copy(alpha = CHIP_FILL_ALPHA),
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = CHIP_BORDER_ALPHA)),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            color = spec.chipText,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
