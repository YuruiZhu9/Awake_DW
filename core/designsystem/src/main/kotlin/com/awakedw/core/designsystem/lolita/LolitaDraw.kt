package com.awakedw.core.designsystem.lolita

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** 描金：软香槟金（蝴蝶结中结），四主题共用（§12）。 */
val GOLD_TRIM = Color(0xFFD9B98A)

/**
 * 蝴蝶结绘制（§12 L1/L2）：左右环扣（主题色）+ 描金中结，可选双垂尾（达标态）。
 * 全部矢量 Path，尺寸由 [width]（结饰总宽）驱动；纯绘制层，不进语义树。
 */
fun DrawScope.drawBow(
    center: Offset,
    width: Float,
    color: Color,
    knotColor: Color = GOLD_TRIM,
    withTails: Boolean = false,
    alpha: Float = 1f,
) {
    val knot = Offset(center.x, center.y)
    if (withTails) {
        drawPath(LolitaBowPaths.tail(knot, width, side = -1f), color.copy(alpha = alpha * 0.85f))
        drawPath(LolitaBowPaths.tail(knot, width, side = 1f), color.copy(alpha = alpha * 0.85f))
    }
    drawPath(LolitaBowPaths.loop(knot, width, side = -1f), color.copy(alpha = alpha))
    drawPath(LolitaBowPaths.loop(knot, width, side = 1f), color.copy(alpha = alpha))
    drawCircle(knotColor.copy(alpha = alpha), radius = width * 0.11f, center = knot)
}
