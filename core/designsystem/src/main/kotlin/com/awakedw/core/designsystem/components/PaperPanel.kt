package com.awakedw.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec

/**
 * A quiet paper surface for secondary information such as charts and timelines.
 * It adds hierarchy without turning every section into a heavy card.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun PaperPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    spec: ThemeSpec = currentThemeSpec(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = spec.chipBg.copy(alpha = 0.58f),
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = 0.36f)),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    color = spec.greetingColor,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}
