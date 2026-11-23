package com.awakedw.core.designsystem.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.ArtworkCrop
import com.awakedw.core.designsystem.lolita.artworkSourceRect
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import kotlin.math.roundToInt

/** 卷首图版高度与框线：方向稿的 104px 按 dp 落地，横裁约 3:1。 */
private val PLATE_HEIGHT = 104.dp
private val PLATE_FRAME = 1.dp

/**
 * 统计页卷首插图（大改版 Phase 2）：同一张主题素材按横裁取一条，装 1dp 框线，
 * 下压一行图注与「卷首」小字。这是「画框里的页」的开口，不是新背景层。
 *
 * 只进 draw 相并随尺寸缓存；素材缺失时留一块安静的纸色空版，版面不跳。
 * 图本身不承载语义，图注是可读文本。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtworkPlate(
    crop: ArtworkCrop,
    caption: String,
    modifier: Modifier = Modifier,
    height: Dp = PLATE_HEIGHT,
    /** 诊断/测试接缝：显式传入位图时优先于主题素材；生产调用保持 null。 */
    image: ImageBitmap? = null,
) {
    val spec = currentThemeSpec()
    val bitmap = image ?: rememberAssetImageOrN(themeArtworkOf(spec.id).asset)
    val ink = spec.greetingSubColor
    Column(modifier = modifier.fillMaxWidth().testTag("artwork-plate")) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .testTag("artwork-plate-image")
                .drawWithCache {
                    val aspect = if (size.height > 0f) size.width / size.height else 3f
                    val src = bitmap?.let { artworkSourceRect(it.width, it.height, crop, aspect) }
                    val frame = PLATE_FRAME.toPx()
                    val frameColor = ink.copy(alpha = 0.34f)
                    val innerHighlight = Color.White.copy(alpha = if (spec.isDark) 0.07f else 0.24f)
                    val fallback = spec.chipBg.copy(alpha = 0.30f)
                    onDrawBehind {
                        if (bitmap != null && src != null) {
                            drawImage(
                                image = bitmap,
                                srcOffset = IntOffset(src.left.roundToInt(), src.top.roundToInt()),
                                srcSize = IntSize(src.width.roundToInt(), src.height.roundToInt()),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                alpha = 0.97f,
                            )
                        } else {
                            drawRect(color = fallback)
                        }
                        // 1dp 框线 + 内侧一线高光：像一张装订进页里的图版。
                        drawRect(color = frameColor, style = Stroke(width = frame))
                        drawRect(
                            color = innerHighlight,
                            topLeft = Offset(frame, frame),
                            size = Size(size.width - frame * 2f, 1.dp.toPx()),
                        )
                    }
                },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = caption,
            color = spec.greetingSubColor.copy(alpha = 0.80f),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(spec.greetingSubColor.copy(alpha = 0.22f)),
        )
    }
}
