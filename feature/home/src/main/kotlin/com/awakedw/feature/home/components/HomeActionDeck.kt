package com.awakedw.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One primary action and two lighter alternatives, without a redundant enclosing card. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun HomeActionDeck(
    cupMl: Int,
    onLog: () -> Unit,
    onQuickLog: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LogButton(onTap = onLog, cupMl = cupMl)
        QuickSipsRow(cupMl = cupMl, onQuickLog = onQuickLog)
    }
}
