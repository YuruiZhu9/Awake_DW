package com.awakedw.core.designsystem.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.awakedw.core.model.ThemeId
import kotlin.math.abs

enum class ParticleStyle { CLASSIC, PEARL, SILVER, PETAL }

fun particleStyleOf(id: ThemeId): ParticleStyle =
    when (id) {
        ThemeId.EMERALD, ThemeId.CARAMEL, ThemeId.THIN_MINT -> ParticleStyle.PEARL
        ThemeId.GOTHIC, ThemeId.NIGHT -> ParticleStyle.SILVER
        ThemeId.CLERIC, ThemeId.STRAWBERRY, ThemeId.LAVENDER -> ParticleStyle.PETAL
    }

/** Leave the central reading column quiet; preserve continuous motion without snapping to side rails. */
internal fun readingColumnAlpha(xFraction: Float): Float = (0.20f + ((abs(xFraction - 0.5f) - 0.18f) / 0.25f).coerceIn(0f, 1f) * 0.80f)

/**
 * SILVER 菱形的复用 Path（2.6.0 绘制审计）：draw 只发生在 UI 线程且同步消费，
 * 每次绘制前 reset 重建即可，哥特/深夜两主题各 80 颗不再逐帧分配。
 */
private val SILVER_MOTE_PATH: Path = Path()

/** Small, code-native accents, not pictorial assets or product state. */
fun DrawScope.drawThemeMote(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
    style: ParticleStyle,
    rotation: Float = 0f,
) {
    val ink = color.copy(alpha = alpha.coerceIn(0f, 1f))
    when (style) {
        ParticleStyle.CLASSIC, ParticleStyle.PEARL -> {
            drawCircle(ink, radius, center)
            drawCircle(
                Color.White.copy(alpha = alpha * 0.65f),
                radius * 0.25f,
                center - Offset(radius * 0.28f, radius * 0.28f),
            )
        }
        ParticleStyle.SILVER -> {
            val diamond = SILVER_MOTE_PATH
            diamond.reset()
            diamond.moveTo(center.x, center.y - radius)
            diamond.quadraticBezierTo(center.x + radius * 0.18f, center.y - radius * 0.18f, center.x + radius * 0.65f, center.y)
            diamond.quadraticBezierTo(center.x + radius * 0.18f, center.y + radius * 0.18f, center.x, center.y + radius)
            diamond.quadraticBezierTo(center.x - radius * 0.18f, center.y + radius * 0.18f, center.x - radius * 0.65f, center.y)
            diamond.quadraticBezierTo(center.x - radius * 0.18f, center.y - radius * 0.18f, center.x, center.y - radius)
            diamond.close()
            drawPath(diamond, ink)
        }
        ParticleStyle.PETAL ->
            rotate(rotation, center) {
                drawOval(ink, center - Offset(radius * 0.45f, radius), Size(radius * 0.9f, radius * 2f))
            }
    }
}
