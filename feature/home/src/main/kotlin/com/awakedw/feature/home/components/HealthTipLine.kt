package com.awakedw.feature.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.RECOMMENDED_MAX_ML
import com.awakedw.core.model.RECOMMENDED_MIN_ML

/**
 * 健康小贴士行（规格 §3.2 第 4 条）：推荐量直接取 core-model 常量插值，不硬编码数字。
 * 布局审计 P3-9：fillMaxWidth + 居中对齐——长句不再左对齐悬挂，与全列中轴对齐。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun HealthTipLine(modifier: Modifier = Modifier) {
    val spec = currentThemeSpec()
    Text(
        text = "📖 《中国居民膳食指南》建议成年人每日饮水 ${RECOMMENDED_MIN_ML}–${RECOMMENDED_MAX_ML}ml",
        color = spec.greetingSubColor,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
