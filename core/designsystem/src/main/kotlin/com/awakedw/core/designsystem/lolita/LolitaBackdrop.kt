package com.awakedw.core.designsystem.lolita

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.model.ThemeId
import kotlin.math.max
import kotlin.math.min

/** Compatibility helper retained for mapping tests. */
internal fun lolitaAssetFileOf(themeId: ThemeId): String = themeArtworkOf(themeId).asset

/**
 * 低存在感的 Lolita 纸面氛围层：只停留在页面背景，不承载任何内容语义。
 *
 * 浅色插画保留纸面乘法混合，已排版的哥特图正常叠加，只有旧深夜墨线需要反相。
 * 竖屏等比铺满避免两侧露出硬边，横屏完整适配避免极端裁切；中央遮罩保护读数。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun LolitaBackdrop(
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
) {
    val art = themeArtworkOf(spec.id)
    val image = rememberAssetImageOrN(art.asset, retainPreviousImage = false)
    val reduceMotion = rememberReduceMotion()
    val reveal =
        animateFloatAsState(
            targetValue = if (image == null) 0f else 1f,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 700),
            label = "lolitaBackdropReveal",
        ).value

    Box(
        modifier =
            modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val source = image
                    if (source == null) {
                        onDrawBehind { if (usesDrawnLaceFrame(spec.id)) drawLaceFrame(spec) }
                    } else {
                        val scale =
                            if (art.framed && size.width > size.height) {
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
                                        spec.backgroundGradient.first().copy(
                                            alpha = art.centerWash,
                                        ),
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
                                0.34f to paper.copy(alpha = art.readingVeil),
                                0.66f to paper.copy(alpha = art.readingVeil),
                                0.82f to paper.copy(alpha = 0.08f),
                                1f to paper.copy(alpha = 0f),
                            )
                        // Invert white-paper art into light ink before screening on a dark surface.
                        val darkInk =
                            if (art.treatment == ArtworkTreatment.INVERTED_INK) {
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
                                alpha = reveal * art.opacity,
                                colorFilter = darkInk,
                                blendMode =
                                    when (art.treatment) {
                                        ArtworkTreatment.PRINTED_INK -> BlendMode.Multiply
                                        ArtworkTreatment.INVERTED_INK -> BlendMode.Screen
                                        ArtworkTreatment.PAINTED -> BlendMode.SrcOver
                                    },
                            )
                            drawRect(brush = centerWash)
                            if (art.framed) {
                                drawRect(brush = readingVeil)
                            }
                            if (usesDrawnLaceFrame(spec.id)) drawLaceFrame(spec)
                        }
                    }
                },
    )
}
