package com.awakedw.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.drawBow

/** 蝴蝶结衣橱入口边长（§5.2 重设计）：24–28dp 取上限，兼顾点按命中率与装饰克制。 */
private val BOW_ENTRY_SIZE = 28.dp

/** 蝴蝶结绘制宽占按钮边长的比例：结饰略小于点击域，留出涟漪与呼吸的边。 */
private const val BOW_ENTRY_DRAW_RATIO = 0.86f

/**
 * 蝴蝶结衣橱入口（§5.2 重设计）：问候语行右端的一枚小蝴蝶结，
 * 蕾丝色环扣 + 描金中结（[drawBow] 默认），静态不摆动（装饰克制）。
 * 今日穿搭的信息回归衣橱页内呈现，首页不再常驻穿搭文字——
 * 点击此处进入衣橱（contentDescription「衣橱」，[onOpenGallery] 既有链路不动）。
 * 交互取最简实现：clickable 默认 indication + 圆形裁形。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun BowEntryButton(
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Canvas(
        modifier =
            modifier
                .size(BOW_ENTRY_SIZE)
                .clip(CircleShape)
                .clickable(role = Role.Button) { onOpenGallery() }
                .semantics { contentDescription = "衣橱" },
    ) {
        drawBow(
            center = Offset(size.width / 2f, size.height / 2f),
            width = size.minDimension * BOW_ENTRY_DRAW_RATIO,
            color = spec.laceColor,
        )
    }
}
