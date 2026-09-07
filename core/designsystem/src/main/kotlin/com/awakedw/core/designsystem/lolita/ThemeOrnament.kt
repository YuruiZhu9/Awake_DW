package com.awakedw.core.designsystem.lolita

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.ornamentColor
import com.awakedw.core.designsystem.particles.ParticleStyle
import com.awakedw.core.designsystem.particles.drawThemeMote
import com.awakedw.core.model.ThemeId

/** One small crest, leaving the photographic border as the main ornament. */
fun DrawScope.drawThemeOrnament(
    center: Offset,
    width: Float,
    spec: ThemeSpec,
    withTails: Boolean = false,
) {
    val ink = ornamentColor(spec)
    when (spec.id) {
        ThemeId.GOTHIC -> {
            val r = width * 0.22f
            val arch =
                Path().apply {
                    moveTo(center.x - r, center.y + r * 0.4f)
                    quadraticBezierTo(center.x - r, center.y - r * 0.5f, center.x, center.y - r)
                    quadraticBezierTo(center.x + r, center.y - r * 0.5f, center.x + r, center.y + r * 0.4f)
                }
            drawPath(arch, ink, style = Stroke(width * 0.028f))
            drawThemeMote(center + Offset(0f, r * 0.16f), r * 0.56f, spec.primary, 0.92f, ParticleStyle.SILVER)
        }
        ThemeId.CLERIC -> {
            drawThemeMote(center, width * 0.20f, ink, 0.94f, ParticleStyle.SILVER)
            for (side in listOf(-1f, 1f)) {
                drawCircle(ink.copy(alpha = 0.65f), width * 0.035f, center + Offset(side * width * 0.32f, 0f))
            }
        }
        ThemeId.THIN_MINT -> {
            // Outline ribbon folds retain the delicacy of cocoa piping instead of three solid dots.
            val r = width * 0.40f
            for (side in listOf(-1f, 1f)) {
                val loop =
                    Path().apply {
                        moveTo(center.x, center.y)
                        cubicTo(
                            center.x + side * r,
                            center.y - r * 0.65f,
                            center.x + side * r * 1.3f,
                            center.y + r * 0.50f,
                            center.x,
                            center.y,
                        )
                    }
                drawPath(loop, spec.chipBg)
                drawPath(loop, ink, style = Stroke(width * 0.028f))
            }
            drawCircle(ink, width * 0.07f, center)
        }
        else -> drawBow(center, width, spec.primary, ink, withTails)
    }
}
