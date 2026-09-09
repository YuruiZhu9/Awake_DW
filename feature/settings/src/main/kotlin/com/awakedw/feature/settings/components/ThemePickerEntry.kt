package com.awakedw.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeById
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.ThemeLaceOverlay
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.model.ThemeChoice

/** Keep the main settings page short, while every theme remains one tap away. */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun ThemePickerEntry(
    selected: ThemeChoice,
    onSelect: (ThemeChoice) -> Unit,
) {
    val spec = currentThemeSpec()
    val selectedSpec = if (selected == ThemeChoice.FOLLOW_TIME) spec else ThemeById.getValue(themeIdOf(selected))
    val selectedArtwork = if (selected == ThemeChoice.FOLLOW_TIME) null else themeArtworkOf(selectedSpec.id)
    val selectedImage = selectedArtwork?.let { rememberAssetImageOrN(it.asset, retainPreviousImage = false) }
    var open by rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = { open = true },
        shape = RoundedCornerShape(16.dp),
        color = spec.ringTrack.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, spec.laceColor.copy(alpha = 0.45f)),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val swatchShape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .size(42.dp)
                    .background(Brush.linearGradient(selectedSpec.backgroundGradient + selectedSpec.primary), swatchShape)
                    .clip(swatchShape),
            ) {
                if (selectedImage != null) {
                    Image(
                        bitmap = selectedImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        alpha = selectedArtwork!!.opacity.coerceAtLeast(0.48f),
                        modifier = Modifier.matchParentSize(),
                    )
                }
                if (selectedArtwork != null) {
                    ThemeLaceOverlay(spec = selectedSpec, modifier = Modifier.matchParentSize(), compact = true)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("主题", color = spec.greetingColor, style = MaterialTheme.typography.titleSmall)
                Text(themeLabel(selected), color = spec.greetingSubColor, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = spec.greetingSubColor)
        }
    }
    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            containerColor = spec.chipBg,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("选择主题", color = spec.greetingColor, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { open = false }) { Text("完成", color = spec.greetingColor) }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
                ThemeChoiceChips(selected = selected, onSelect = onSelect)
            }
        }
    }
}
