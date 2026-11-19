package com.awakedw.core.designsystem.paper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 纸面层参数（大改版 Phase 1，方向稿 §3.3 的 D+ 初值）。
 *
 * 纸从「背景滤镜」变成页面主体：版心接近实底，页边向主题场景透出；
 * 三个核心旋钮（[paperEdgeAlpha]、[ghostOpacity]、[foreEdgeWidth]）是真机
 * 校准的第一顺位——先降水印，再降纸边透明度，正文对比度永远优先。
 *
 * 这些值只作用于 `EditorialSheet` 与首页版心，统计/设置仍走既有纸面。
 */
@Immutable
data class PaperTokens(
    /** 纸左右各留出的「桌面」：主题场景从这一条露出，文字边距不变。 */
    val insetHorizontal: Dp = 10.dp,
    /** 版心纸面不透明度：阅读列几乎实底。 */
    val paperCenterAlpha: Float = 0.975f,
    /** 页边纸面不透明度：D+ 取 0.88；不要低于 0.86。 */
    val paperEdgeAlpha: Float = 0.88f,
    /**
     * 左右呼吸带宽度：纸从页边 88% 渐回版心 97.5% 的过渡距离。
     * 与 10dp 桌面合计 36dp 视觉带宽；正文从 28dp 起，首字落在约 95% 的纸上。
     */
    val edgeBandWidth: Dp = 26.dp,
    /** 页口发丝区宽度：落在内容边距以内，不挤正文。 */
    val foreEdgeWidth: Dp = 18.dp,
    /** 纸上角花水印不透明度：D+ 取 0.19；超过 0.18 视作明显版，抢字即回 0.14。 */
    val ghostOpacity: Float = 0.19f,
    /** 水印绘制区边长：超出纸右上角的部分裁掉。 */
    val ghostSize: Dp = 220.dp,
    /** 纤维噪点不透明度：只进 draw 相，一次性路径。 */
    val grainAlpha: Float = 0.045f,
) {
    companion object {
        val Default = PaperTokens()
    }
}
