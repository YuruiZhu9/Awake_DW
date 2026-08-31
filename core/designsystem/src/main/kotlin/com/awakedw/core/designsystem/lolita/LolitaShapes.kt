package com.awakedw.core.designsystem.lolita

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * 缎带横幅形（§12 L1）：主体矩形 + 两端下垂燕尾、外缘中央 V 形剪口。
 * [tailFraction] 为单侧燕尾占宽比例。
 */
class RibbonBannerShape(
    private val tailFraction: Float = 0.10f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val t = w * tailFraction
        val path =
            Path().apply {
                moveTo(0f, h * 0.12f)
                lineTo(t, 0f)
                lineTo(w - t, 0f)
                lineTo(w, h * 0.12f)
                lineTo(w - t * 0.45f, h * 0.5f)
                lineTo(w, h * 0.88f)
                lineTo(w - t, h)
                lineTo(t, h)
                lineTo(0f, h * 0.88f)
                lineTo(t * 0.45f, h * 0.5f)
                close()
            }
        return Outline.Generic(path)
    }
}

/** 蝴蝶结/飘带的路径装配（§12）：左右环扣对称生成，供 [drawBow] 与测试共用。 */
internal object LolitaBowPaths {
    /** 单侧环扣路径（[side] = -1 左 / +1 右，以结心 [knot] 为原点对称）。 */
    fun loop(
        knot: Offset,
        width: Float,
        side: Float,
    ): Path =
        Path().apply {
            moveTo(knot.x, knot.y - width * 0.02f)
            cubicTo(
                knot.x + side * width * 0.16f,
                knot.y - width * 0.30f,
                knot.x + side * width * 0.50f,
                knot.y - width * 0.26f,
                knot.x + side * width * 0.48f,
                knot.y - width * 0.02f,
            )
            cubicTo(
                knot.x + side * width * 0.50f,
                knot.y + width * 0.24f,
                knot.x + side * width * 0.16f,
                knot.y + width * 0.28f,
                knot.x,
                knot.y + width * 0.12f,
            )
            close()
        }

    /** 单侧垂尾路径（达标态出现，挂在结心下方外侧）。 */
    fun tail(
        knot: Offset,
        width: Float,
        side: Float,
    ): Path =
        Path().apply {
            moveTo(knot.x + side * width * 0.06f, knot.y + width * 0.08f)
            lineTo(knot.x + side * width * 0.26f, knot.y + width * 0.52f)
            lineTo(knot.x + side * width * 0.06f, knot.y + width * 0.42f)
            close()
        }

    /** 缩放包围盒自检用：单侧环扣最远端横向偏移比例。 */
    const val LOOP_EXTENT_FRACTION: Float = 0.50f
}
