package com.awakedw.core.designsystem.lolita

import androidx.compose.ui.geometry.Rect

/**
 * 主题图裁切（大改版 Phase 1）：把同一张「画框」素材按角色取不同角。
 *
 * [focusX]/[focusY] 是原图内归一化焦点（0..1），[zoom] 是放大倍率——2.0 表示
 * 取原图短边的 1/2 宽。八主题各自挑最耐看的角：窗环看细节、水印看角花、
 * 统计卷首看横裁，全部走这一套参数，不新增素材。
 */
data class ArtworkCrop(
    val focusX: Float,
    val focusY: Float,
    val zoom: Float,
) {
    init {
        require(zoom > 0f) { "zoom must be positive" }
    }
}

/**
 * 正方形裁切源矩形（窗环/水印）：以焦点为中心取原图短边的 1/[zoom] 正方形，
 * 并夹回图内，保证任何焦点/倍率都不会越界。返回像素坐标。
 */
fun artworkSourceRect(
    imageWidth: Int,
    imageHeight: Int,
    crop: ArtworkCrop,
): Rect = artworkSourceRect(imageWidth, imageHeight, crop, aspectRatio = 1f)

/**
 * 任意宽高比的裁切源矩形（统计页卷首是横裁）：先按原图与目标宽高比取最大内接矩形，
 * 再按 [crop.zoom] 缩小并以焦点为中心，最后夹回图内。`aspectRatio` 是目标宽/高；
 * 取 1f 时与正方形重载逐位一致。
 */
fun artworkSourceRect(
    imageWidth: Int,
    imageHeight: Int,
    crop: ArtworkCrop,
    aspectRatio: Float,
): Rect {
    require(aspectRatio > 0f) { "aspectRatio must be positive" }
    val width = imageWidth.coerceAtLeast(1)
    val height = imageHeight.coerceAtLeast(1)
    val imageAspect = width.toFloat() / height
    val maxWidth: Float
    val maxHeight: Float
    if (imageAspect >= aspectRatio) {
        maxHeight = height.toFloat()
        maxWidth = maxHeight * aspectRatio
    } else {
        maxWidth = width.toFloat()
        maxHeight = maxWidth / aspectRatio
    }
    val cropWidth = (maxWidth / crop.zoom).coerceIn(1f, maxWidth)
    val cropHeight = (maxHeight / crop.zoom).coerceIn(1f, maxHeight)
    val centerX = crop.focusX.coerceIn(0f, 1f) * width
    val centerY = crop.focusY.coerceIn(0f, 1f) * height
    val left = (centerX - cropWidth / 2f).coerceIn(0f, width - cropWidth)
    val top = (centerY - cropHeight / 2f).coerceIn(0f, height - cropHeight)
    return Rect(left, top, left + cropWidth, top + cropHeight)
}
