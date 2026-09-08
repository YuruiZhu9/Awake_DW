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
            val diamond =
                Path().apply {
                    moveTo(center.x, center.y - radius)
                    quadraticBezierTo(center.x + radius * 0.18f, center.y - radius * 0.18f, center.x + radius * 0.65f, center.y)
                    quadraticBezierTo(center.x + radius * 0.18f, center.y + radius * 0.18f, center.x, center.y + radius)
                    quadraticBezierTo(center.x - radius * 0.18f, center.y + radius * 0.18f, center.x - radius * 0.65f, center.y)
                    quadraticBezierTo(center.x - radius * 0.18f, center.y - radius * 0.18f, center.x, center.y - radius)
                    close()
                }
            drawPath(diamond, ink)
        }
        ParticleStyle.PETAL ->
            rotate(rotation, center) {
                drawOval(ink, center - Offset(radius * 0.45f, radius), Size(radius * 0.9f, radius * 2f))
            }
    }
}
