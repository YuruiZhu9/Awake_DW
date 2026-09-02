package com.awakedw.core.designsystem.lolita

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec

/**
 * A quiet lace-and-pearl rule used to give a page one structural Lolita detail.
 * It is decorative only: no click target, no status, and no product meaning.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun LolitaRule(
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
) {
    val spec = currentThemeSpec()
    val lace = lineColor ?: spec.laceColor
    Canvas(modifier = modifier.fillMaxWidth().height(12.dp)) {
        val centerY = size.height / 2f
        val centerX = size.width / 2f
        val sideInset = 14.dp.toPx()
        val ornamentGap = 12.dp.toPx()
        drawLine(
            color = lace.copy(alpha = 0.52f),
            start = Offset(sideInset, centerY),
            end = Offset(centerX - ornamentGap, centerY),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = lace.copy(alpha = 0.52f),
            start = Offset(centerX + ornamentGap, centerY),
            end = Offset(size.width - sideInset, centerY),
            strokeWidth = 1.dp.toPx(),
        )
        drawCircle(color = GOLD_TRIM.copy(alpha = 0.82f), radius = 2.5.dp.toPx(), center = Offset(centerX, centerY))
        drawCircle(color = lace.copy(alpha = 0.74f), radius = 1.5.dp.toPx(), center = Offset(centerX - 7.dp.toPx(), centerY))
        drawCircle(color = lace.copy(alpha = 0.74f), radius = 1.5.dp.toPx(), center = Offset(centerX + 7.dp.toPx(), centerY))
    }
}
