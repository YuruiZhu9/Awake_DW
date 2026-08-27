package com.awakedw.core.designsystem.particles

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/** 噪点网格间距：4px 微点纹理（设计规格 §2.2）。 */
internal val GRAIN_CELL = 4.dp

/** 单颗微点直径。 */
internal val GRAIN_DOT = 1.dp

/**
 * 独立噪点颗粒层（规格 §2.2 multiply 点阵）。
 *
 * 注意：[com.awakedw.core.designsystem.GradientBackdrop] 内部已自带同款颗粒层，
 * 本组件仅供**没有**铺底座（backdrop）的表面使用，切勿叠用以免噪点浓度翻倍。
 *
 * @param alpha 颗粒不透明度，默认约 5%
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun GrainOverlay(
    modifier: Modifier,
    alpha: Float = 0.05f,
) {
    Box(
        modifier =
            modifier.drawWithCache {
                val grain = buildGrainPath(size, GRAIN_CELL.toPx(), GRAIN_DOT.toPx())
                onDrawBehind { drawPath(grain, Color.Black, alpha = alpha, blendMode = BlendMode.Multiply) }
            },
    )
}

/** 确定性构建 4px 错位网格的微点路径；尺寸变化时随 drawWithCache 重算一次。 */
internal fun buildGrainPath(
    bounds: Size,
    cellPx: Float,
    dotPx: Float,
): Path =
    Path().apply {
        var row = 0
        var y = cellPx / 2f
        while (y < bounds.height + dotPx) {
            val rowOffset = if (row % 2 == 0) 0f else cellPx / 2f
            var x = cellPx / 2f + rowOffset
            while (x < bounds.width + dotPx) {
                addOval(Rect(center = Offset(x, y), radius = dotPx / 2f))
                x += cellPx
            }
            y += cellPx
            row++
        }
    }
