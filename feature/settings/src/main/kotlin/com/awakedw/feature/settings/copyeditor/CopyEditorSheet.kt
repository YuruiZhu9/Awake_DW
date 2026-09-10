package com.awakedw.feature.settings.copyeditor

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.components.AwakeConfirmDialog
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.model.TimeSlot
import com.awakedw.feature.settings.COPY_MAX_CHARS

/** 文案分组胶囊圆角。 */
private val GROUP_SHAPE: Shape = RoundedCornerShape(18.dp)

/** 编辑对话框卡片圆角。 */
private val DIALOG_SHAPE: Shape = RoundedCornerShape(24.dp)

/** 超过这个字数就在编辑框里提示一句：首页问候会多占一两行（不做截断）。 */
private const val GREETING_COMFORT_CHARS = 20

/** 分区说明：讲清这份库到底管哪两处，避免使用者以为改它会影响猫说什么。 */
internal const val COPY_SCOPE_SUBTITLE = "用于首页问候与提醒通知"

/** 页脚：反向说明打卡确认与猫咪回应不在这份库里。 */
internal const val COPY_SCOPE_FOOTER = "打卡后的确认语和猫咪回应用的是内置短句，不在这里编辑。"

/** 分组内的操作说明：长按是隐藏手势，必须写出来。 */
internal const val COPY_ITEM_HINT = "点一下改，长按删除"

/** 折叠组元信息：时段 → 展示名（早/午/晚）。 */
private data class GroupMeta(
    val slot: TimeSlot,
    val label: String,
)

private val GROUPS: List<GroupMeta> =
    listOf(
        GroupMeta(TimeSlot.MORNING, "早上"),
        GroupMeta(TimeSlot.DAY, "白天"),
        GroupMeta(TimeSlot.EVENING, "晚上"),
    )

private fun groupLabelOf(slot: TimeSlot): String = GROUPS.firstOrNull { it.slot == slot }?.label.orEmpty()

/**
 * 「心意文案库」分区卡（§3.4）：早/午/晚三个可折叠分组；条目点击弹编辑对话框
 * （TextField ≤ [COPY_MAX_CHARS] 字）、长按删除（带确认，删错了整库恢复默认也找不回自写内容）；
 * 组内「＋ 新增一句」；右上「恢复默认」带确认 Dialog。全部改动即时落库，无保存键。
 *
 * 库里只放**长句**——首页问候与提醒通知正文。打卡确认与猫咪回应用内置短句池，
 * 因此分区说明与页脚都把这层分工讲清楚，避免使用者以为改动会影响猫说什么。
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
    // 待确认的删除：长按不直接删，先确认再落库。
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "心意文案库", color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
                // P3-6：删除/编辑说明一句放在分区头部，不再每条句子尾随常驻「长按删除」小字。
                Text(
                    text = COPY_SCOPE_SUBTITLE,
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Box(
                modifier =
                    Modifier
                        .sizeIn(minWidth = ControlMinHeight, minHeight = ControlMinHeight)
                        .clickable(onClick = { resetConfirming = true }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "恢复默认",
                    color = spec.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Spacer(Modifier.padding(top = 6.dp))
        GROUPS.forEach { group ->
            val items = library.groupOf(group.slot)
            CopyGroup(
                label = group.label,
                items = items,
                onEdit = { index -> editing = EditingTarget(group.slot, index, items[index], isNew = false) },
                onAdd = { editing = EditingTarget(group.slot, items.size, "", isNew = true) },
                onDelete = { index -> pendingDelete = PendingDelete(group.slot, index, items[index]) },
            )
        }
        Text(
            text = COPY_SCOPE_FOOTER,
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 10.dp),
        )
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

    pendingDelete?.let { target ->
        AwakeConfirmDialog(
            title = "删除这句话",
            body = "「${target.text}」会从${groupLabelOf(target.slot)}组移除。恢复默认会把整库换回出厂文案，自己写的内容不会保留。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                onDelete(target.slot, target.index)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (resetConfirming) {
        AwakeConfirmDialog(
            title = "恢复默认",
            body = "整库会换成出厂的 108 句，自己新增和修改过的内容都会丢失。继续吗？",
            confirmLabel = "恢复默认",
            destructive = true,
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

/** 待确认的删除目标：带上句子本身，确认框里能把要删的内容念出来。 */
private data class PendingDelete(
    val slot: TimeSlot,
    val index: Int,
    val text: String,
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
                modifier = Modifier.fillMaxWidth().heightIn(min = ControlMinHeight).clickable(onClick = { expanded = !expanded }),
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
                // 触控高度走 ControlMinHeight：此前只有 10dp 内边距，实际可点高度约 36dp。
                Text(
                    text = "＋ 新增一句",
                    color = spec.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAdd)
                            .heightIn(min = ControlMinHeight)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(vertical = 10.dp),
                )
                if (items.isEmpty()) {
                    Text(
                        text = "还没有内容，添加一句吧",
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        text = COPY_ITEM_HINT,
                        color = spec.greetingSubColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
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

/** 单句条目：点击编辑、长按删除（规格 §3.4）；操作说明收在分组内一行小字，行内不再常驻提示。 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CopyItemRow(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val spec = currentThemeSpec()
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        color = spec.chipText,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ControlMinHeight)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClickLabel = "删除这句话",
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongClick()
                    },
                )
                .semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("删除这句话") {
                                onLongClick()
                                true
                            },
                        )
                }
                .padding(vertical = 7.dp),
    )
}

/** 句子编辑对话框：TextField 限 [COPY_MAX_CHARS] 字，空文本不可保存；偏长时提示会多占行。 */
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
                Text(text = "编辑这句话", color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
                BasicTextField(
                    value = text,
                    onValueChange = { next -> if (next.length <= COPY_MAX_CHARS) text = next },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = spec.chipText),
                    cursorBrush = SolidColor(spec.primary),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = "写一句简单的话…",
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
                    val oversize = text.length > GREETING_COMFORT_CHARS
                    Text(
                        text =
                            if (oversize) {
                                "${text.length}/$COPY_MAX_CHARS · 稍长，首页会多占一行"
                            } else {
                                "${text.length}/$COPY_MAX_CHARS"
                            },
                        color = if (oversize) spec.primary else spec.greetingSubColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DialogTextAction(label = "取消", color = spec.greetingSubColor, onClick = onDismiss)
                        DialogTextAction(
                            label = "保存",
                            color = if (text.isNotBlank()) spec.primary else spec.greetingSubColor,
                            onClick = { if (text.isNotBlank()) onConfirm(text) },
                        )
                    }
                }
            }
        }
    }
}

/** 对话框里的文字动作：补足 48dp 触控高度，避免贴边的细字难以点中。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun DialogTextAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = ControlMinHeight).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = color, style = MaterialTheme.typography.labelLarge)
    }
}
