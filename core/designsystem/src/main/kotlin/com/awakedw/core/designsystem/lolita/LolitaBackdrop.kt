package com.awakedw.core.designsystem.lolita

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private const val ARTWORK_ROTATION_MS = 12_000L
private const val ARTWORK_CROSSFADE_MS = 900

/** Compatibility helper retained for mapping tests. */
internal fun lolitaAssetFileOf(themeId: ThemeId): String = themeArtworkOf(themeId).asset

/**
 * Low-presence local atmosphere layer. It never carries content meaning.
 * Existing same-theme candidates rotate every 12 seconds in normal motion mode;
 * reduced motion keeps the configured primary image still.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun LolitaBackdrop(
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
) {
    val artwork = themeArtworkOf(spec.id)
    val reduceMotion = rememberReduceMotion()
    val cycle = remember(artwork) { artworkCycleOf(artwork) }
    var assetIndex by remember(artwork) { mutableIntStateOf(0) }

    LaunchedEffect(artwork, reduceMotion, cycle) {
        assetIndex = 0
        if (!reduceMotion && cycle.size > 1) {
            while (true) {
                delay(ARTWORK_ROTATION_MS)
                assetIndex = (assetIndex + 1) % cycle.size
            }
        }
    }

    val selectedAsset = cycle.getOrElse(assetIndex) { artwork.asset }
    val primaryImage = rememberAssetImageOrN(artwork.asset, retainPreviousImage = false)
    val selectedImage =
        if (selectedAsset == artwork.asset) {
            primaryImage
        } else {
            rememberAssetImageOrN(selectedAsset, retainPreviousImage = false)
        }
    val image = selectedImage ?: primaryImage

    Crossfade(
        targetState = image,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else ARTWORK_CROSSFADE_MS),
        label = "lolitaBackdropArtwork",
    ) { source ->
        val reveal =
            animateFloatAsState(
                targetValue = if (source == null) 0f else 1f,
                animationSpec = tween(durationMillis = if (reduceMotion) 0 else 700),
                label = "lolitaBackdropReveal",
            ).value
        ArtworkLayer(
            source = source,
            artwork = artwork,
            spec = spec,
            reveal = reveal,
            modifier = modifier,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ArtworkLayer(
    source: ImageBitmap?,
    artwork: ThemeArtwork,
    spec: ThemeSpec,
    reveal: Float,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    if (source == null) {
                        onDrawBehind { if (usesDrawnLaceFrame(spec.id)) drawLaceFrame(spec) }
                    } else {
                        val scale =
                            if (artwork.framed && size.width > size.height) {
                                min(size.width / source.width, size.height / source.height)
                            } else {
                                max(size.width / source.width, size.height / source.height)
                            }
                        val dstWidth = (source.width * scale).toInt().coerceAtLeast(1)
                        val dstHeight = (source.height * scale).toInt().coerceAtLeast(1)
                        val dstOffsetX = ((size.width - dstWidth) / 2f).toInt()
                        val dstOffsetY = ((size.height - dstHeight) / 2f).toInt()
                        val centerWash =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        spec.backgroundGradient.first().copy(alpha = artwork.centerWash),
                                        Color.Transparent,
                                    ),
                                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.48f),
                                radius = max(size.width, size.height) * 0.66f,
                            )
                        // Frame detail remains at the sides; only the reading column receives a veil.
                        val paper = spec.backgroundGradient[1]
                        val readingVeil =
                            Brush.horizontalGradient(
                                0f to paper.copy(alpha = 0f),
                                0.18f to paper.copy(alpha = 0.08f),
                                0.34f to paper.copy(alpha = artwork.readingVeil),
                                0.66f to paper.copy(alpha = artwork.readingVeil),
                                0.82f to paper.copy(alpha = 0.08f),
                                1f to paper.copy(alpha = 0f),
                            )
                        // Invert white-paper art into light ink before screening on a dark surface.
                        val darkInk =
                            if (artwork.treatment == ArtworkTreatment.INVERTED_INK) {
                                ColorFilter.colorMatrix(
                                    ColorMatrix(
                                        floatArrayOf(
                                            -1f, 0f, 0f, 0f, 255f,
                                            0f, -1f, 0f, 0f, 255f,
                                            0f, 0f, -1f, 0f, 255f,
                                            0f, 0f, 0f, 1f, 0f,
                                        ),
                                    ),
                                )
                            } else {
                                null
                            }
                        onDrawBehind {
                            drawImage(
                                image = source,
                                dstOffset = androidx.compose.ui.unit.IntOffset(dstOffsetX, dstOffsetY),
                                dstSize = androidx.compose.ui.unit.IntSize(dstWidth, dstHeight),
                                alpha = reveal * artwork.opacity,
                                colorFilter = darkInk,
                                blendMode =
                                    when (artwork.treatment) {
                                        ArtworkTreatment.PRINTED_INK -> BlendMode.Multiply
                                        ArtworkTreatment.INVERTED_INK -> BlendMode.Screen
                                        ArtworkTreatment.PAINTED -> BlendMode.SrcOver
                                    },
                            )
                            drawRect(brush = centerWash)
                            if (artwork.framed) drawRect(brush = readingVeil)
                            if (usesDrawnLaceFrame(spec.id)) drawLaceFrame(spec)
                        }
                    }
                },
    )
}
