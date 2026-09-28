package com.awakedw.feature.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Quick amounts stay full-width below the hero row; they remain a secondary path. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun HomeQuickAmounts(
    cupMl: Int,
    onQuickLog: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickSipsRow(cupMl = cupMl, onQuickLog = onQuickLog, modifier = modifier)
}
