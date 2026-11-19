package com.awakedw.core.designsystem.ring

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.awakedw.core.designsystem.lolita.ArtworkCrop

/**
 * 环内窗景（大改版 Phase 1）：把主题图的一角放大垫在刻度与值弧之下。
 *
 * 环仍是「环即把手」——窗景只改变看起来的样子，不接管任何手势。
 * [veilAlpha] 让环心文字始终有纸一样的底：默认 0.20 露出角花，
 * 环心出现引文/拖环预览时由调用方抬到 0.8 左右。
 * [medallionAlpha] 控制中心纸面圆牌的显隐，同样由调用方按状态淡变。
 */
@Immutable
data class RingLens(
    val image: ImageBitmap,
    val crop: ArtworkCrop,
    val veilAlpha: Float = 0.20f,
    val medallionAlpha: Float = 1f,
)
