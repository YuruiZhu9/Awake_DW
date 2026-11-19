package com.awakedw.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.currentThemeSpec

/** 分节微标签的字距：小、疏、静，Nike 式层级耳语（1.5.0 统计页定稿）。 */
private const val SECTION_LABEL_SPACING = 2.5f

/**
 * 分节微标签 + 右延发丝线：统计页与设置页共用的版面骨架。
 *
 * 1.5.0 去卡片化后，页面靠「微标签 + 发丝线 + 留白」分节而不是靠盒子。
 * [trailing] 放分节级动作（如「恢复默认」）；没有动作时发丝线一直延到右缘。
 * 这次从统计页私有实现提升为共享件（rules §二.2：第二处使用即下沉），
 * 统计页的像素输出保持逐位不变。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun EditorialSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val spec = currentThemeSpec()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = SECTION_LABEL_SPACING.sp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(spec.laceColor.copy(alpha = 0.30f)))
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
    }
}
