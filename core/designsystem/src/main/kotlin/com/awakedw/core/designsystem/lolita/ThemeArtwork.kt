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
        // 1.4.0 背景透明度：主图全线降档（真机反馈「透明度太浅」），纸色透出来、画当氛围不当照片；
        // 画框主题（哥特/圣职/薄巧）降幅保守——框内细节密度高，透太多会碎。
        ThemeId.EMERALD ->
            ThemeArtwork(
                asset = "lolita/morning_blue_porcelain.jpg",
                opacity = 0.56f,
                treatment = ArtworkTreatment.PAINTED,
                midgroundAsset = "lolita/morning_midground.webp",
            )
        ThemeId.STRAWBERRY ->
            ThemeArtwork(
                asset = "lolita/afternoon_lotus.jpg",
                opacity = 0.55f,
                treatment = ArtworkTreatment.PAINTED,
                midgroundAsset = "lolita/afternoon_midground.webp",
            )
        ThemeId.CARAMEL ->
            ThemeArtwork(
                asset = "lolita/twilight_milk_tea.jpg",
                opacity = 0.54f,
                treatment = ArtworkTreatment.PAINTED,
                midgroundAsset = "lolita/twilight_midground.webp",
            )
        ThemeId.NIGHT ->
            ThemeArtwork(
                asset = "lolita/midnight_indigo.jpg",
                opacity = 0.58f,
                treatment = ArtworkTreatment.PAINTED,
                midgroundAsset = "lolita/night_midground.webp",
                midgroundOpacity = 0.55f,
            )
        ThemeId.LAVENDER ->
            ThemeArtwork(
                asset = "lolita/mist_lavender_rose.jpg",
                opacity = 0.55f,
                treatment = ArtworkTreatment.PAINTED,
                midgroundAsset = "lolita/lavender_midground.webp",
            )
        ThemeId.GOTHIC ->
            ThemeArtwork(
                asset = "lolita/gothic_frame.jpg",
                opacity = 0.66f,
                treatment = ArtworkTreatment.PAINTED,
                framed = true,
                midgroundAsset = "lolita/gothic_midground.webp",
                midgroundOpacity = 0.45f,
            )
        ThemeId.CLERIC ->
            ThemeArtwork(
                asset = "lolita/cleric.jpg",
                opacity = 0.82f,
                treatment = ArtworkTreatment.PAINTED,
                framed = true,
                centerWash = 0.10f,
                readingVeil = 0.48f,
                midgroundAsset = "lolita/cleric_midground.webp",
                midgroundOpacity = 0.45f,
            )
        ThemeId.THIN_MINT ->
            ThemeArtwork(
                asset = "lolita/thin_mint.jpg",
                opacity = 0.52f,
                treatment = ArtworkTreatment.PAINTED,
                framed = true,
                midgroundAsset = "lolita/thin_mint_midground.webp",
                midgroundOpacity = 0.45f,
            )
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
