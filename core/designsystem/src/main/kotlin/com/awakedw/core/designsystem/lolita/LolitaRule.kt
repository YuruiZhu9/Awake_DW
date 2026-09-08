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
        drawThemeOrnament(Offset(centerX, centerY), 20.dp.toPx(), spec)
        // Fine paired seams repeat the frame's embroidery without adding another border.
        for (side in listOf(-1f, 1f)) {
            drawLine(
                lace.copy(alpha = 0.22f),
                Offset(centerX + side * 24.dp.toPx(), centerY + 3.dp.toPx()),
                Offset(centerX + side * (centerX - sideInset), centerY + 3.dp.toPx()),
                strokeWidth = 0.5.dp.toPx(),
            )
            drawCircle(lace.copy(alpha = 0.72f), 1.2.dp.toPx(), Offset(centerX + side * 17.dp.toPx(), centerY))
        }
    }
}
