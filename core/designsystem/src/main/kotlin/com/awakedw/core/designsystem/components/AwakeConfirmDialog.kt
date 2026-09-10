package com.awakedw.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.currentThemeSpec

/** 确认对话框卡片圆角，与文案编辑对话框同一语言。 */
private val DIALOG_SHAPE = RoundedCornerShape(24.dp)

/**
 * 项目内统一的轻量确认对话框。
 *
 * 用于所有「做了就收不回」的操作：撤回一杯水、删除一句文案、整库恢复默认。
 * 视觉沿用纸面卡 + 主题色细描边；两个动作都保证 ≥ [ControlMinHeight] 的触控目标，
 * 危险动作（[destructive]）用主题 primary 强调，其余动作保持次级色的安静。
 *
 * @param onConfirm 确认动作；调用方负责在此关闭对话框（避免双重关闭竞态）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AwakeConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = "取消",
    destructive: Boolean = false,
) {
    val spec = currentThemeSpec()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = DIALOG_SHAPE, color = spec.chipBg, modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                Text(text = title, color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    color = spec.chipText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogAction(
                        label = cancelLabel,
                        color = spec.greetingSubColor,
                        onClick = onDismiss,
                    )
                    DialogAction(
                        label = confirmLabel,
                        color = if (destructive) spec.primary else spec.chipText,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

/** 对话框动作：48dp 触控目标 + 最小 64dp 宽度，避免贴边的细字难以点中。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun DialogAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = 64.dp, minHeight = ControlMinHeight)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
