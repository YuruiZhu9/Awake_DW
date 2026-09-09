package com.awakedw.core.designsystem.lolita

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec

/** Static, palette-colored embroidery. No input, semantics, bitmaps or animation clock. */
internal fun DrawScope.drawLaceFrame(spec: ThemeSpec) {
    drawLaceFrame(spec = spec, compact = false)
}

private fun DrawScope.drawLaceFrame(
    spec: ThemeSpec,
    compact: Boolean,
) {
    val ink = spec.laceColor
    val inset = if (compact) 2.dp.toPx() else 5.dp.toPx()
    val depth = if (compact) 3.5.dp.toPx() else 9.dp.toPx()
    val start = if (compact) 4.dp.toPx() else 40.dp.toPx()
    val end = size.height - if (compact) 4.dp.toPx() else 28.dp.toPx()
    val stitch = if (compact) 0.45.dp.toPx() else 0.65.dp.toPx()
    if (size.width < (if (compact) 24.dp.toPx() else 80.dp.toPx()) || end <= start) return
    val repeats = ((end - start) / if (compact) 12.dp.toPx() else 22.dp.toPx()).toInt().coerceAtLeast(1)
    val step = (end - start) / repeats
    for (side in listOf(-1f, 1f)) {
        val edge = if (side < 0f) inset else size.width - inset
        val inward = -side
        drawLine(ink.copy(alpha = if (compact) 0.32f else 0.40f), Offset(edge, start), Offset(edge, end), stitch)
        val scallops = Path()
        repeat(repeats) { index ->
            val y = start + index * step
            scallops.moveTo(edge, y)
            scallops.cubicTo(edge + inward * depth, y + step * 0.20f, edge + inward * depth, y + step * 0.80f, edge, y + step)
            drawCircle(
                ink.copy(alpha = if (compact) 0.48f else 0.65f),
                if (compact) 0.65.dp.toPx() else 1.dp.toPx(),
                Offset(edge + inward * (depth + if (compact) 1.dp.toPx() else 2.dp.toPx()), y + step * 0.5f),
            )
        }
        drawPath(scallops, ink.copy(alpha = if (compact) 0.36f else 0.48f), style = Stroke(stitch))
        if (!compact) {
            // A small ribbon at each shoulder, never a horizontal bar over the page heading.
            val knot = Offset(edge + inward * 9.dp.toPx(), 24.dp.toPx())
            drawBow(knot, 23.dp.toPx(), spec.primary.copy(alpha = 0.60f), ink.copy(alpha = 0.72f), withTails = true)
            drawCircle(ink.copy(alpha = 0.72f), 1.5.dp.toPx(), Offset(edge, end + 6.dp.toPx()))
        }
    }
}

/**
 * Shared code-native lace for theme previews and full-page fallback paths.
 * Dedicated picture frames intentionally render nothing here to avoid double framing.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun ThemeLaceOverlay(
    spec: ThemeSpec = currentThemeSpec(),
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (usesDrawnLaceFrame(spec.id)) {
        Canvas(modifier) {
            drawLaceFrame(spec, compact = compact)
        }
    }
}
