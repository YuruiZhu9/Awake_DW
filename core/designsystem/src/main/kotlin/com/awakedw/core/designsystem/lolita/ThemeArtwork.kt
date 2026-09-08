package com.awakedw.core.designsystem.lolita

import com.awakedw.core.model.ThemeId

/** Runtime art is local and stable: never randomly replaced during a gesture or a recomposition. */
data class ThemeArtwork(
    val asset: String,
    val opacity: Float,
    val treatment: ArtworkTreatment = ArtworkTreatment.PRINTED_INK,
    val framed: Boolean = false,
    val centerWash: Float = if (framed) 0.30f else 0.14f,
    val readingVeil: Float = 0.70f,
)

enum class ArtworkTreatment { PRINTED_INK, INVERTED_INK, PAINTED }

fun themeArtworkOf(id: ThemeId): ThemeArtwork =
    when (id) {
        ThemeId.EMERALD -> ThemeArtwork("lolita/blue.jpg", 0.24f)
        ThemeId.STRAWBERRY -> ThemeArtwork("lolita/rose.jpg", 0.18f)
        ThemeId.CARAMEL -> ThemeArtwork("lolita/warm.jpg", 0.18f)
        ThemeId.NIGHT -> ThemeArtwork("lolita/gothic.jpg", 0.20f, ArtworkTreatment.INVERTED_INK)
        ThemeId.LAVENDER -> ThemeArtwork("lolita/blue.jpg", 0.18f)
        ThemeId.GOTHIC -> ThemeArtwork("lolita/gothic_frame.jpg", 0.80f, ArtworkTreatment.PAINTED, framed = true)
        ThemeId.CLERIC ->
            ThemeArtwork(
                "lolita/cleric.jpg",
                0.96f,
                ArtworkTreatment.PAINTED,
                framed = true,
                centerWash = 0.10f,
                readingVeil = 0.48f,
            )
        ThemeId.THIN_MINT -> ThemeArtwork("lolita/thin_mint.jpg", 0.60f, framed = true)
    }

/** Stronger text surfaces for detailed art; legacy themes retain their original translucency. */
fun artworkPanelOpacity(
    id: ThemeId,
    fallback: Float,
): Float = if (themeArtworkOf(id).framed) 0.94f else fallback
