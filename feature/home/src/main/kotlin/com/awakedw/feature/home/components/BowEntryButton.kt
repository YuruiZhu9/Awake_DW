package com.awakedw.feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.GOLD_TRIM
import com.awakedw.core.designsystem.lolita.drawBow

/** 蝴蝶结衣橱入口边长（§5.2 重设计）：24–28dp 取上限，兼顾点按命中率与装饰克制。 */
private val BOW_ENTRY_SIZE = 28.dp

/** 蝴蝶结绘制宽占按钮边长的比例：结饰略小于点击域，留出涟漪与呼吸的边。 */
private const val BOW_ENTRY_DRAW_RATIO = 0.86f

/** 未看新解锁描金圆点直径：小小的「有新裙」信号，克制不喧哗。 */
private val UNSEEN_DOT_SIZE = 6.dp

/** 未看圆点 scale-in 时长（用户裁定：出现即 200ms 缩放浮现）。 */
private const val UNSEEN_DOT_SCALE_IN_MS = 200

/** 未看圆点内缩量：沿圆裁形边内收，避免被入口的圆形裁形削角。 */
private val UNSEEN_DOT_INSET = 2.dp

/** 未看圆点语义描述：测试与无障碍共用（有新藏品未看）。 */
internal const val UNSEEN_DOT_DESCRIPTION = "新裙入柜"

/**
 * 蝴蝶结衣橱入口（§5.2 重设计）：问候语行右端的一枚小蝴蝶结，
 * 蕾丝色环扣 + 描金中结（[drawBow] 默认），静态不摆动（装饰克制）。
 * 今日穿搭的信息回归衣橱页内呈现，首页不再常驻穿搭文字——
 * 点击此处进入衣橱（contentDescription「衣橱」，[onOpenGallery] 既有链路不动）。
 * 交互取最简实现：clickable 默认 indication + 圆形裁形。
 *
 * 「新裙提示」（用户裁定「无声等待制」首页唯一一层）：[showDot] 为 true 时
 * 右上角亮一枚 6dp 描金圆点（[GOLD_TRIM]，200ms scale-in 浮现），无未看藏品不渲染；
 * 已读清账发生在画廊（GalleryViewModel init），本层只管亮与灭。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun BowEntryButton(
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
) {
    val spec = currentThemeSpec()
    Box(
        modifier =
            modifier
                .size(BOW_ENTRY_SIZE)
                .clip(CircleShape)
                .clickable(role = Role.Button) { onOpenGallery() }
                .semantics { contentDescription = "衣橱" },
    ) {
        Canvas(modifier = Modifier.size(BOW_ENTRY_SIZE)) {
            drawBow(
                center = Offset(size.width / 2f, size.height / 2f),
                width = size.minDimension * BOW_ENTRY_DRAW_RATIO,
                color = spec.laceColor,
            )
        }
        if (showDot) {
            val dotScale = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                dotScale.animateTo(1f, tween(durationMillis = UNSEEN_DOT_SCALE_IN_MS, easing = FastOutSlowInEasing))
            }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = -UNSEEN_DOT_INSET, y = UNSEEN_DOT_INSET)
                        .size(UNSEEN_DOT_SIZE)
                        .graphicsLayer {
                            scaleX = dotScale.value
                            scaleY = dotScale.value
                        }
                        .background(color = GOLD_TRIM, shape = CircleShape)
                        .semantics { contentDescription = UNSEEN_DOT_DESCRIPTION },
            )
        }
    }
}
