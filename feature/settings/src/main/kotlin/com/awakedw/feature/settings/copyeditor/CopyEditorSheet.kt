package com.awakedw.feature.settings.copyeditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.model.TimeSlot
import com.awakedw.feature.settings.COPY_MAX_CHARS

/** 文案分组胶囊圆角。 */
private val GROUP_SHAPE: Shape = RoundedCornerShape(18.dp)

/** 编辑对话框卡片圆角。 */
private val DIALOG_SHAPE: Shape = RoundedCornerShape(24.dp)

/** 折叠组元信息：时段 → 展示名（早/午/晚）。 */
private data class GroupMeta(
    val slot: TimeSlot,
    val label: String,
)

private val GROUPS: List<GroupMeta> =
    listOf(
        GroupMeta(TimeSlot.MORNING, "早安"),
        GroupMeta(TimeSlot.DAY, "午后"),
        GroupMeta(TimeSlot.EVENING, "晚安"),
    )

/**
 * 「心意文案库」分区卡（§3.4）：早/午/晚三个可折叠分组；条目点击弹编辑对话框
 * （TextField ≤ [COPY_MAX_CHARS] 字）、长按删除；组内「＋ 新增一句」；
 * 右上「恢复默认文案」带确认 Dialog。全部改动即时落库，无保存键。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun CopyLibrarySection(
    library: CopyLibrary,
    onUpsert: (slot: TimeSlot, index: Int, text: String) -> Unit,
    onAdd: (slot: TimeSlot, text: String) -> Unit,
    onDelete: (slot: TimeSlot, index: Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    var resetConfirming by remember { mutableStateOf(false) }
    // 正在编辑的条目：null 表示对话框关闭。
    var editing by remember { mutableStateOf<EditingTarget?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "心意文案库", color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
                Text(text = "每一句都写给自己", color = spec.greetingSubColor, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = "恢复默认文案",
                color = spec.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable(onClick = { resetConfirming = true }),
            )
        }
        Spacer(Modifier.padding(top = 6.dp))
        GROUPS.forEach { group ->
            val items = library.groupOf(group.slot)
            CopyGroup(
                label = group.label,
                items = items,
                onEdit = { index -> editing = EditingTarget(group.slot, index, items[index], isNew = false) },
                onAdd = { editing = EditingTarget(group.slot, items.size, "", isNew = true) },
                onDelete = { index -> onDelete(group.slot, index) },
            )
        }
    }

    editing?.let { target ->
        CopyEditDialog(
            initialText = target.initialText,
            onConfirm = { text ->
                if (target.isNew) {
                    onAdd(target.slot, text)
                } else {
                    onUpsert(target.slot, target.index, text)
                }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (resetConfirming) {
        ConfirmDialog(
            title = "恢复默认文案",
            body = "会把所有句子换回出厂默认，现在改过的就不留了，确定吗？",
            confirmLabel = "恢复默认",
            onConfirm = {
                onReset()
                resetConfirming = false
            },
            onDismiss = { resetConfirming = false },
        )
    }
}

/** 正在编辑的条目：[isNew] 为 true 时保存走「追加」语义。 */
private data class EditingTarget(
    val slot: TimeSlot,
    val index: Int,
    val initialText: String,
    val isNew: Boolean,
)

/** 单个折叠分组：组头（时段名 + 句数 + 箭头）＋ 展开时的「＋ 新增一句」（置顶）与句子列表。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CopyGroup(
    label: String,
    items: List<String>,
    onEdit: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
) {
    val spec = currentThemeSpec()
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(shape = GROUP_SHAPE, color = spec.chipText.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = { expanded = !expanded }),
            ) {
                Text(text = label, color = spec.greetingColor, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.padding(start = 6.dp))
                Text(
                    text = "${items.size} 句",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = spec.chipText,
                    modifier = Modifier.rotate(if (expanded) 0f else -90f),
                )
            }
            if (expanded) {
                Spacer(Modifier.padding(top = 4.dp))
                // §3.4：「＋ 新增一句」置每组展开区顶部，新增路径永远一步可达。
                Text(
                    text = "＋ 新增一句",
                    color = spec.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAdd)
                            .padding(bottom = 6.dp),
                )
                if (items.isEmpty()) {
                    Text(
                        text = "这里还空着，加一句吧",
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    items.forEachIndexed { index, text ->
                        CopyItemRow(
                            text = text,
                            onClick = { onEdit(index) },
                            onLongClick = { onDelete(index) },
                        )
                    }
                }
            }
        }
    }
}

/** 单句条目：点击编辑、长按删除（规格 §3.4）。 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CopyItemRow(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val spec = currentThemeSpec()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 7.dp),
    ) {
        Text(text = text, color = spec.chipText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = "长按删除", color = spec.greetingSubColor, style = MaterialTheme.typography.labelSmall)
    }
}

/** 句子编辑对话框：TextField 限 [COPY_MAX_CHARS] 字，空文本不可保存。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CopyEditDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val spec = currentThemeSpec()
    var text by remember { mutableStateOf(initialText.take(COPY_MAX_CHARS)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = DIALOG_SHAPE, color = spec.chipBg, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "编辑这一句", color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
                BasicTextField(
                    value = text,
                    onValueChange = { next -> if (next.length <= COPY_MAX_CHARS) text = next },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = spec.chipText),
                    cursorBrush = SolidColor(spec.primary),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = "写一句温柔的话…",
                                    color = spec.greetingSubColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${text.length}/$COPY_MAX_CHARS",
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "取消",
                            color = spec.greetingSubColor,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable(onClick = onDismiss),
                        )
                        Text(
                            text = "保存",
                            color = if (text.isNotBlank()) spec.primary else spec.greetingSubColor,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable(onClick = { if (text.isNotBlank()) onConfirm(text) }),
                        )
                    }
                }
            }
        }
    }
}

/** 通用确认对话框（「恢复默认文案」用）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spec = currentThemeSpec()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = DIALOG_SHAPE, color = spec.chipBg, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = title, color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
                Text(text = body, color = spec.chipText, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "取消",
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable(onClick = onDismiss).padding(end = 20.dp),
                    )
                    Text(
                        text = confirmLabel,
                        color = spec.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable(onClick = onConfirm),
                    )
                }
            }
        }
    }
}
