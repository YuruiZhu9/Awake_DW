package com.awakedw.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
                PanelTitle(title = title, spec = spec)
                Spacer(Modifier.height(11.dp))
            }
            content()
        }
    }
}

/** A small editorial title rail: one Lolita detail, kept subordinate to the data. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun PanelTitle(
    title: String,
    spec: ThemeSpec,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier =
                Modifier
                    .width(18.dp)
                    .height(1.dp)
                    .background(spec.laceColor.copy(alpha = 0.72f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = spec.greetingColor,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.width(7.dp))
        Box(
            modifier =
                Modifier
                    .size(4.dp)
                    .background(spec.primary.copy(alpha = 0.72f), CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Spacer(
            modifier =
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(spec.laceColor.copy(alpha = 0.22f)),
        )
    }
}
