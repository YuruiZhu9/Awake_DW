package com.awakedw.core.designsystem.lolita

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec

/** Static, palette-colored embroidery. No input, semantics, bitmaps or animation clock. */
internal fun DrawScope.drawLaceFrame(spec: ThemeSpec) {
    val ink = spec.laceColor
    val inset = 5.dp.toPx()
    val depth = 9.dp.toPx()
    val start = 40.dp.toPx()
    val end = size.height - 28.dp.toPx()
    val stitch = 0.65.dp.toPx()
    if (size.width < 80.dp.toPx() || end <= start) return
    val repeats = ((end - start) / 22.dp.toPx()).toInt().coerceAtLeast(1)
    val step = (end - start) / repeats
    for (side in listOf(-1f, 1f)) {
        val edge = if (side < 0f) inset else size.width - inset
        val inward = -side
        drawLine(ink.copy(alpha = 0.40f), Offset(edge, start), Offset(edge, end), stitch)
        val scallops = Path()
        repeat(repeats) { index ->
            val y = start + index * step
            scallops.moveTo(edge, y)
            scallops.cubicTo(edge + inward * depth, y + step * 0.20f, edge + inward * depth, y + step * 0.80f, edge, y + step)
            drawCircle(ink.copy(alpha = 0.65f), 1.dp.toPx(), Offset(edge + inward * (depth + 2.dp.toPx()), y + step * 0.5f))
        }
        drawPath(scallops, ink.copy(alpha = 0.48f), style = Stroke(stitch))
        // A small ribbon at each shoulder, never a horizontal bar over the page heading.
        val knot = Offset(edge + inward * 9.dp.toPx(), 24.dp.toPx())
        drawBow(knot, 23.dp.toPx(), spec.primary.copy(alpha = 0.60f), ink.copy(alpha = 0.72f), withTails = true)
        drawCircle(ink.copy(alpha = 0.72f), 1.5.dp.toPx(), Offset(edge, end + 6.dp.toPx()))
    }
}
