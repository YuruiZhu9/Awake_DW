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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.model.ThemeId
import kotlin.math.max

/** 主题 → 用户提供的 Lolita 氛围素材。文件放在 app/src/main/assets/lolita/。 */
internal fun lolitaAssetFileOf(themeId: ThemeId): String =
    when (themeId) {
        ThemeId.EMERALD -> "lolita/green.jpg"
        ThemeId.STRAWBERRY -> "lolita/rose.jpg"
        ThemeId.CARAMEL -> "lolita/warm.jpg"
        ThemeId.NIGHT -> "lolita/gothic.jpg"
        ThemeId.LAVENDER -> "lolita/blue.jpg"
    }

/**
 * 低存在感的 Lolita 纸面氛围层：只停留在页面背景，不承载任何内容语义。
 *
 * 用户素材本身带有大面积留白，因此使用乘法混合叠在主题渐变上：白色背景不会把页面
 * 洗成一整块，蕾丝、蝴蝶结和花朵只在边缘留下轻微的纸面印记。中心再加一层主题色
 * 留白，确保进度环、按钮、图表和设置文字始终是第一信息层级。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun LolitaBackdrop(
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
) {
    val image = rememberAssetImageOrN(lolitaAssetFileOf(spec.id))
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
                        onDrawBehind { }
                    } else {
                        val scale = max(size.width / source.width, size.height / source.height)
                        val dstWidth = (source.width * scale).toInt().coerceAtLeast(1)
                        val dstHeight = (source.height * scale).toInt().coerceAtLeast(1)
                        val dstOffsetX = ((size.width - dstWidth) / 2f).toInt()
                        val dstOffsetY = ((size.height - dstHeight) / 2f).toInt()
                        val centerWash =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        spec.backgroundGradient.first().copy(alpha = if (spec.isDark) 0.12f else 0.20f),
                                        Color.Transparent,
                                    ),
                                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.48f),
                                radius = max(size.width, size.height) * 0.66f,
                            )
                        onDrawBehind {
                            drawImage(
                                image = source,
                                dstOffset = androidx.compose.ui.unit.IntOffset(dstOffsetX, dstOffsetY),
                                dstSize = androidx.compose.ui.unit.IntSize(dstWidth, dstHeight),
                                alpha = reveal * if (spec.isDark) 0.16f else 0.13f,
                                blendMode = BlendMode.Multiply,
                            )
                            drawRect(brush = centerWash)
                        }
                    }
                },
    )
}
