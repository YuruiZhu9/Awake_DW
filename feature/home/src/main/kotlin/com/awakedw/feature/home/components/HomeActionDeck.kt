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

/** Editorial 2.0 primary action: kept beside the hero so the main action is never pushed below the instrument. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun EditorialPrimaryAction(
    cupMl: Int,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        LogButton(onTap = onLog, cupMl = null)
        androidx.compose.material3.Text(
            text = "一杯 ${cupMl}ml",
            color = com.awakedw.core.designsystem.currentThemeSpec().greetingSubColor,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}

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
