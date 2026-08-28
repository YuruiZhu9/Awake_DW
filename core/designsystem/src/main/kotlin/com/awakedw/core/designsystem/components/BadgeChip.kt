package com.awakedw.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec

/** 徽章胶囊圆角：全圆。 */
private val CHIP_SHAPE: Shape = RoundedCornerShape(percent = 50)

/**
 * 徽章胶囊（首页 §3.2 / 统计页 §3.3 共用的唯一实现）：
 * chipBg 全圆底 + chipText 小字。样式只此一份，避免两处复制漂移。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun BadgeChip(
    text: String,
    spec: ThemeSpec,
) {
    Surface(shape = CHIP_SHAPE, color = spec.chipBg) {
        Text(
            text = text,
            color = spec.chipText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}
