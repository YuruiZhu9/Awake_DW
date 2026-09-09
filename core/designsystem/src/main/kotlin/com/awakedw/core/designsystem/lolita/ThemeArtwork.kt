package com.awakedw.core.designsystem.lolita

import com.awakedw.core.model.ThemeId
import kotlin.random.Random

/** Local theme atmosphere art. The primary image is shown first; candidates rotate slowly. */
data class ThemeArtwork(
    val asset: String,
    val opacity: Float,
    val treatment: ArtworkTreatment = ArtworkTreatment.PRINTED_INK,
    val framed: Boolean = false,
    val centerWash: Float = if (framed) 0.30f else 0.14f,
    val readingVeil: Float = 0.70f,
    val candidateAssets: List<String> = listOf(asset),
) {
    /** Keep the primary asset first and remove blank or duplicate candidates. */
    fun usableAssets(): List<String> =
        listOf(asset)
            .plus(candidateAssets)
            .filter(String::isNotBlank)
            .distinct()
}

enum class ArtworkTreatment { PRINTED_INK, INVERTED_INK, PAINTED }

fun themeArtworkOf(id: ThemeId): ThemeArtwork =
    when (id) {
        ThemeId.EMERALD ->
            ThemeArtwork(
                "lolita/blue.jpg",
                0.24f,
                candidateAssets = listOf("lolita/blue_alt.jpg", "lolita/blue_soft.jpg"),
            )
        ThemeId.STRAWBERRY ->
            ThemeArtwork(
                "lolita/rose.jpg",
                0.18f,
                candidateAssets = listOf("lolita/rose_alt.jpg", "lolita/rose_soft.jpg"),
            )
        ThemeId.CARAMEL ->
            ThemeArtwork(
                "lolita/warm.jpg",
                0.18f,
                candidateAssets = listOf("lolita/warm_alt.jpg"),
            )
        ThemeId.NIGHT -> ThemeArtwork("lolita/gothic.jpg", 0.20f, ArtworkTreatment.INVERTED_INK)
        ThemeId.LAVENDER ->
            ThemeArtwork(
                "lolita/blue.jpg",
                0.18f,
                candidateAssets = listOf("lolita/lavender.jpg"),
            )
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

/** Stable pseudo-random order; recomposition cannot reshuffle the artwork. */
internal fun artworkCycleOf(artwork: ThemeArtwork): List<String> {
    val assets = artwork.usableAssets()
    if (assets.size <= 1) return assets
    val alternates = assets.drop(1).shuffled(Random(0x41 + artwork.asset.hashCode()))
    return listOf(assets.first()) + alternates
}

/** Detailed art uses opaque content surfaces; legacy themes retain their original translucency. */
fun artworkPanelOpacity(
    id: ThemeId,
    fallback: Float,
): Float = if (themeArtworkOf(id).framed) 0.94f else fallback

/** Existing picture frames stay intact; all other themes receive a palette-colored lace hem. */
internal fun usesDrawnLaceFrame(id: ThemeId): Boolean = !themeArtworkOf(id).framed
