package com.awakedw.core.designsystem.art

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.Outfit
import kotlin.math.roundToInt

/** 换图交叉淡入时长（moodboard §5.1）：600ms。 */
private const val CROSSFADE_MS = 600

/**
 * 画卷层默认 alpha（主题感知档位，P3-10 校准）：
 * 浅色主题 0.30f（Multiply 正片叠底），深色主题 0.22f（SrcOver 薄纱罩）。
 */
internal fun backdropAlpha(isDark: Boolean): Float = if (isDark) 0.22f else 0.30f

/**
 * 画卷层混合模式（P3-10）：深夜近黑底上 Multiply 会把画卷乘成近似纯黑（不可见病灶），
 * 改 SrcOver（常规透明合成）以低 alpha 薄纱罩铺在暗底上；浅色主题维持 Multiply 压出纸感。
 */
internal fun backdropBlendMode(isDark: Boolean): BlendMode = if (isDark) BlendMode.SrcOver else BlendMode.Multiply

/** 画卷层实际生效的 alpha：显式覆盖 [alphaOverride] 优先，否则按主题档位 [backdropAlpha]。 */
internal fun resolveBackdropAlpha(
    isDark: Boolean,
    alphaOverride: Float?,
): Float = alphaOverride ?: backdropAlpha(isDark)

/**
 * ContentScale.Crop 语义的源区裁切矩形：把源图按等比放大铺满目标后取居中窗口，
 * 即 [drawImage] 的 `srcOffset`/`srcSize`——铺满、不变形、居中。
 */
internal fun backdropCropSrc(
    imageWidth: Int,
    imageHeight: Int,
    dstWidth: Int,
    dstHeight: Int,
): IntRect {
    val scale = maxOf(dstWidth.toFloat() / imageWidth, dstHeight.toFloat() / imageHeight)
    val srcWidth = (dstWidth / scale).toInt().coerceAtLeast(1)
    val srcHeight = (dstHeight / scale).toInt().coerceAtLeast(1)
    return IntRect(
        offset = IntOffset((imageWidth - srcWidth) / 2, (imageHeight - srcHeight) / 2),
        size = IntSize(srcWidth, srcHeight),
    )
}

/**
 * 全屏画卷底层（moodboard §5.1）：置于背景渐变之上、内容之下。
 * 浅色主题 alpha=0.30f + Multiply；深色主题 alpha=0.22f + SrcOver（近黑底上 Multiply
 * 会把画卷乘成纯黑，见 P3-10）；换图 600ms 交叉淡入；夜变体优先；资产缺失不绘制（回退纯渐变）。
 *
 * 注意：纸纹由全局背景底座统一负责——真实噪点烘焙在 `Backdrop.kt` 的 `GradientBackdrop`
 * 离屏图层内，本层不重复叠加，以免噪点浓度翻倍。位图按 ContentScale.Crop 语义铺满并居中；
 * 交叉淡入以**位图**为目标态而非 [outfit]——资产尚未就位（null）不参与过渡，避免闪跳。
 *
 * @param outfit 今日之裙；null 或资产缺失/解码失败时不绘制任何图像。
 * @param alphaOverride 覆盖主题档位 alpha 的显式值（测试/特殊版面用），null 时按主题取 0.30f/0.22f。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun DressBackdrop(
    outfit: Outfit?,
    modifier: Modifier = Modifier,
    alphaOverride: Float? = null,
) {
    val context = LocalContext.current
    val isDark = currentThemeSpec().isDark

    // 夜变体解析：深色主题下映射出 _night 路径并按存在性取舍（存在则用之，否则原文件）。
    val resolvedAsset =
        remember(outfit?.assetFile, isDark) {
            outfit?.assetFile?.let { assetFile ->
                if (isDark) nightVariantOf(context, assetFile) else assetFile
            }
        }
    val imageBitmap = if (resolvedAsset != null) rememberAssetImageOrN(resolvedAsset) else null

    Crossfade(
        targetState = imageBitmap,
        animationSpec = tween(durationMillis = CROSSFADE_MS),
        label = "DressBackdropCrossfade",
        modifier = modifier,
    ) { bitmap ->
        if (bitmap != null) {
            val alpha = resolveBackdropAlpha(isDark, alphaOverride)
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val src = backdropCropSrc(bitmap.width, bitmap.height, size.width.roundToInt(), size.height.roundToInt())
                            onDrawBehind {
                                drawImage(
                                    image = bitmap,
                                    srcOffset = src.topLeft,
                                    srcSize = src.size,
                                    dstOffset = IntOffset.Zero,
                                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    alpha = alpha,
                                    blendMode = backdropBlendMode(isDark),
                                )
                            }
                        },
            )
        }
    }
}
