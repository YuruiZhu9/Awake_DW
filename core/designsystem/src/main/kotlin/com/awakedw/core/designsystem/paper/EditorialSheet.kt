package com.awakedw.core.designsystem.paper

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.artworkSourceRect
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.designsystem.particles.GRAIN_CELL
import com.awakedw.core.designsystem.particles.GRAIN_DOT
import com.awakedw.core.designsystem.particles.buildGrainPath
import kotlin.math.roundToInt

/** 纸边压痕线宽与书口内细分线间距。 */
private val PAPER_EDGE_LINE = 1.dp
private val FORE_EDGE_LINE_GAP = 4.dp

/** 桌面投影条宽：纸外极轻的一层压暗，替代被禁用的高斯模糊。 */
private val DESK_SHADOW_WIDTH = 6.dp

/** 纸色：浅色主题取背景第二档向纸白提亮，深色主题只提 4.5% 白，保住夜读的暗面。 */
fun paperColorOf(spec: ThemeSpec): Color {
    val base = spec.backgroundGradient.getOrElse(1) { spec.backgroundGradient.first() }
    return if (spec.isDark) {
        lerp(base, Color.White, 0.045f)
    } else {
        lerp(base, Color.White, 0.62f)
    }
}

/**
 * 首页纸面层（大改版 Phase 1）：一张铺在主题场景上的纸。
 *
 * 绘制顺序：桌面投影 → 纸面横向 alpha 呼吸带 → 右上角花水印（19%，
 * 径向遮罩淡出）→ 程序化纤维噪点 → 纸边压痕 / 页口细线 / 左侧订口。
 * 全部只进 draw 相：无位图上传、无逐帧分配、无模糊滤镜；素材缺失时
 * 水印整层静默，纸仍在。
 *
 * 本层不持有语义，也不承载业务；内容由页面在自己的列里叠放。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun EditorialSheet(
    modifier: Modifier = Modifier,
    tokens: PaperTokens = PaperTokens.Default,
    /** 诊断/测试接缝：显式传入角花位图时优先于主题素材；生产调用保持 null。 */
    ghostImage: ImageBitmap? = null,
) {
    val spec = currentThemeSpec()
    val artwork = themeArtworkOf(spec.id)
    val crops = editorialCropsOf(spec.id)
    val image = ghostImage ?: rememberAssetImageOrN(artwork.asset)
    val paper = paperColorOf(spec)
    val ink = spec.greetingSubColor

    Box(
        modifier =
            modifier.drawWithCache {
                val inset = tokens.insetHorizontal.toPx()
                val paperRect = Rect(inset, 0f, size.width - inset, size.height)
                if (paperRect.width <= 1f) return@drawWithCache onDrawBehind { }

                // 呼吸带（D+ 关键修正）：纸面不做大半径径向渐变——那会让左右边永远走不到
                // 页边 alpha。改用横向渐变，左右各 26dp 从 88% 渐回 97.5%，版心仍是实底，
                // 场景确实从纸边透出来；上下缘出血，不需要渐变。
                val edgeBandFraction = (tokens.edgeBandWidth.toPx() / paperRect.width).coerceIn(0f, 0.42f)
                val paperGradient =
                    Brush.horizontalGradient(
                        colorStops =
                            arrayOf(
                                0f to paper.copy(alpha = tokens.paperEdgeAlpha),
                                edgeBandFraction to paper.copy(alpha = tokens.paperCenterAlpha),
                                1f - edgeBandFraction to paper.copy(alpha = tokens.paperCenterAlpha),
                                1f to paper.copy(alpha = tokens.paperEdgeAlpha),
                            ),
                        startX = paperRect.left,
                        endX = paperRect.right,
                    )

                val grain =
                    buildGrainPath(
                        Size(paperRect.width, paperRect.height),
                        GRAIN_CELL.toPx(),
                        GRAIN_DOT.toPx(),
                    )

                val ghostSizePx = minOf(tokens.ghostSize.toPx(), paperRect.width * 0.74f)
                val ghostRect =
                    Rect(
                        left = paperRect.right - ghostSizePx * 0.82f,
                        top = paperRect.top - ghostSizePx * 0.10f,
                        right = paperRect.right + ghostSizePx * 0.18f,
                        bottom = paperRect.top + ghostSizePx * 0.90f,
                    )
                val ghostLayer = ghostRect.intersect(paperRect)
                val ghostMaskCenter = Offset(ghostRect.center.x, ghostRect.top + ghostRect.height * 0.36f)
                val ghostMaskRadius = ghostSizePx * 0.62f
                val ghostSrc = image?.let { artworkSourceRect(it.width, it.height, crops.ghost) }

                val shadowWidth = DESK_SHADOW_WIDTH.toPx()
                val edgeWidth = PAPER_EDGE_LINE.toPx()
                val foreEdge = tokens.foreEdgeWidth.toPx()
                val lineGap = FORE_EDGE_LINE_GAP.toPx()
                // 静态笔刷/颜色一次构建：onDrawBehind 每帧只铺命令，不再分配列表与笔刷。
                val leftShadowBrush = Brush.horizontalGradient(listOf(ink.copy(alpha = 0.10f), Color.Transparent))
                val rightShadowBrush = Brush.horizontalGradient(listOf(Color.Transparent, ink.copy(alpha = 0.10f)))
                val gutterBrush =
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (spec.isDark) 0.16f else 0.035f),
                            Color.Transparent,
                        ),
                    )
                val edgeHighlight = Color.White.copy(alpha = if (spec.isDark) 0.09f else 0.32f)
                val edgeCrease = ink.copy(alpha = 0.12f)

                onDrawBehind {
                    // 桌面投影：纸两侧 6dp 的极轻压暗；无模糊，只做一层方向性渐变。
                    drawRect(
                        brush = leftShadowBrush,
                        topLeft = Offset(paperRect.left - shadowWidth, 0f),
                        size = Size(shadowWidth, size.height),
                    )
                    drawRect(
                        brush = rightShadowBrush,
                        topLeft = Offset(paperRect.right, 0f),
                        size = Size(shadowWidth, size.height),
                    )

                    drawRect(
                        brush = paperGradient,
                        topLeft = paperRect.topLeft,
                        size = paperRect.size,
                    )

                    clipRect(paperRect.left, paperRect.top, paperRect.right, paperRect.bottom) {
                        // 右上角花水印：放大裁切一角，径向遮罩淡出；深色主题改正常混合以保可见。
                        if (image != null && ghostSrc != null) {
                            val canvas = drawContext.canvas
                            canvas.saveLayer(ghostLayer, Paint())
                            drawImage(
                                image = image,
                                srcOffset = IntOffset(ghostSrc.left.roundToInt(), ghostSrc.top.roundToInt()),
                                srcSize = IntSize(ghostSrc.width.roundToInt(), ghostSrc.height.roundToInt()),
                                dstOffset = IntOffset(ghostRect.left.roundToInt(), ghostRect.top.roundToInt()),
                                dstSize = IntSize(ghostRect.width.roundToInt(), ghostRect.height.roundToInt()),
                                alpha = tokens.ghostOpacity,
                            )
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colorStops =
                                            arrayOf(
                                                0f to Color.Black,
                                                0.55f to Color.Black.copy(alpha = 0.62f),
                                                1f to Color.Transparent,
                                            ),
                                        center = ghostMaskCenter,
                                        radius = ghostMaskRadius,
                                    ),
                                topLeft = ghostLayer.topLeft,
                                size = ghostLayer.size,
                                blendMode = BlendMode.DstIn,
                            )
                            canvas.restore()
                        }

                        // 纤维噪点：确定性路径，浅色 multiply、深色 screen，只铺在纸内。
                        translate(left = paperRect.left, top = paperRect.top) {
                            drawPath(
                                path = grain,
                                color = if (spec.isDark) Color.White else Color.Black,
                                alpha = if (spec.isDark) tokens.grainAlpha * 0.55f else tokens.grainAlpha,
                                blendMode = if (spec.isDark) BlendMode.Screen else BlendMode.Multiply,
                            )
                        }
                    }

                    // 纸边：外侧一线高光 + 内侧压痕；左右对称。
                    drawPaperEdge(x = paperRect.left, highlight = edgeHighlight, crease = edgeCrease, width = edgeWidth)
                    drawPaperEdge(x = paperRect.right, highlight = edgeHighlight, crease = edgeCrease, width = edgeWidth)

                    // 订口：左缘向内 12dp 的极轻暗面，让纸像被装订而不是贴上去。
                    drawRect(
                        brush = gutterBrush,
                        topLeft = paperRect.topLeft,
                        size = Size(12.dp.toPx(), paperRect.height),
                    )

                    // 书口：右缘 18dp 内的页口发丝线，根的线更深，越外越轻。
                    drawLine(
                        color = ink.copy(alpha = 0.12f),
                        start = Offset(paperRect.right - foreEdge, 0f),
                        end = Offset(paperRect.right - foreEdge, size.height),
                        strokeWidth = edgeWidth,
                    )
                    for (index in 1..3) {
                        val x = paperRect.right - index * lineGap
                        drawLine(
                            color = ink.copy(alpha = 0.05f + index * 0.012f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 0.7.dp.toPx(),
                        )
                    }
                }
            },
    )
}

/** 单侧纸边：外侧 1dp 高光 + 内侧 1dp 压痕；写成 draw 扩展，逐帧不分配。 */
private fun DrawScope.drawPaperEdge(
    x: Float,
    highlight: Color,
    crease: Color,
    width: Float,
) {
    drawLine(
        color = highlight,
        start = Offset(x - width / 2f, 0f),
        end = Offset(x - width / 2f, size.height),
        strokeWidth = width,
    )
    drawLine(
        color = crease,
        start = Offset(x + width / 2f, 0f),
        end = Offset(x + width / 2f, size.height),
        strokeWidth = width,
    )
}
