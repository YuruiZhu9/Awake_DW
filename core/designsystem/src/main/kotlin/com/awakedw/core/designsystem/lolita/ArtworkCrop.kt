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
 * 裁切源矩形（纯函数，便于 JVM 测试）：以焦点为中心取原图短边的 1/[zoom] 正方形，
 * 并夹回图内，保证任何焦点/倍率都不会越界。返回像素坐标。
 */
fun artworkSourceRect(
    imageWidth: Int,
    imageHeight: Int,
    crop: ArtworkCrop,
): Rect {
    val width = imageWidth.coerceAtLeast(1)
    val height = imageHeight.coerceAtLeast(1)
    val side = (minOf(width, height) / crop.zoom).coerceIn(1f, minOf(width, height).toFloat())
    val centerX = crop.focusX.coerceIn(0f, 1f) * width
    val centerY = crop.focusY.coerceIn(0f, 1f) * height
    val left = (centerX - side / 2f).coerceIn(0f, width - side)
    val top = (centerY - side / 2f).coerceIn(0f, height - side)
    return Rect(left, top, left + side, top + side)
}
