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
    /** 中景装饰层（alpha13 §13）：透明 PNG，null 表示该主题暂无分层素材。 */
    val midgroundAsset: String? = null,
    /** 中景层基础不透明度，与时段氛围（SceneSpec.midgroundAlpha）相乘生效。 */
    val midgroundOpacity: Float = 0.55f,
) {
    /** Keep the primary asset first and remove blank or duplicate candidates. */
    fun usableAssets(): List<String> =
        listOf(asset)
            .plus(candidateAssets)
            .filter(String::isNotBlank)
            .distinct()
}

enum class ArtworkTreatment { PRINTED_INK, PAINTED }

fun themeArtworkOf(id: ThemeId): ThemeArtwork =
    when (id) {
        ThemeId.EMERALD ->
            ThemeArtwork(
                asset = "lolita/morning_blue_porcelain.jpg",
                opacity = 0.74f,
                treatment = ArtworkTreatment.PAINTED,
            )
        ThemeId.STRAWBERRY ->
            ThemeArtwork(
                asset = "lolita/afternoon_lotus.jpg",
                opacity = 0.72f,
                treatment = ArtworkTreatment.PAINTED,
            )
        ThemeId.CARAMEL ->
            ThemeArtwork(
                asset = "lolita/twilight_milk_tea.jpg",
                opacity = 0.70f,
                treatment = ArtworkTreatment.PAINTED,
            )
        ThemeId.NIGHT ->
            ThemeArtwork(
                asset = "lolita/midnight_indigo.jpg",
                opacity = 0.72f,
                treatment = ArtworkTreatment.PAINTED,
                midgroundAsset = "lolita/night_midground.png",
                midgroundOpacity = 0.55f,
            )
        ThemeId.LAVENDER ->
            ThemeArtwork(
                asset = "lolita/mist_lavender_rose.jpg",
                opacity = 0.72f,
                treatment = ArtworkTreatment.PAINTED,
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
        ThemeId.THIN_MINT -> ThemeArtwork("lolita/thin_mint.jpg", 0.60f, ArtworkTreatment.PAINTED, framed = true)
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
