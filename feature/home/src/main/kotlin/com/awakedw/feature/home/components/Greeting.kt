package com.awakedw.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.TimeSlot
import java.time.LocalDateTime

/** 问候语文字区右侧让位：为装饰锚点保留避让带，但不改变问候语的主层级。 */
private val GREETING_BOW_CLEARANCE = 36.dp

/** 时段问候语（规格 §3.2 第 1 条：早/午/晚分组内置）。 */
internal fun greetingFor(slot: TimeSlot): String =
    when (slot) {
        TimeSlot.MORNING -> "早安，先喝一口水"
        TimeSlot.DAY -> "今天也请清清爽爽"
        TimeSlot.EVENING -> "晚上好，今天辛苦啦"
    }

/** 日期副行：达标前「距离目标还有 Yml」，达标后「已完成 X%」（规格 §3.2 示例）。 */
internal fun dateSubline(
    now: LocalDateTime,
    totalMl: Int,
    goalMl: Int,
): String {
    val date = "${now.monthValue}月${now.dayOfMonth}日"
    return if (totalMl >= goalMl) {
        val percent = if (goalMl <= 0) 100 else minOf(100, totalMl * 100 / goalMl)
        "$date · 已完成 $percent%"
    } else {
        "$date · 距离目标还有 ${goalMl - totalMl}ml"
    }
}

/**
 * 时段问候 + 日期副行（规格 §3.2 自上而下第 1 条）。
 * [customGreeting] 非空时优先展示（文案库抽取，每次进首页都换一句），
 * null 时回落时段默认句；日期读系统本地时间，随一天自然流转。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun Greeting(
    customGreeting: String?,
    totalMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val now = remember { LocalDateTime.now() }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = customGreeting ?: greetingFor(TimeSlots.slotOfHour(now.hour)),
            color = spec.greetingColor,
            // 问候语用系统衬线（§12 L2）：古典洛丽塔的书卷气，随 ROM 落到宋体/思源宋。
            // 布局审计 P1-3：headlineSmall 24sp 下 12–20 字句在 360dp 屏必折行且第一行行尾
            // 被蝴蝶结压住——降为 titleLarge 22sp、至多两行（溢出省略），文字区右让位 36dp 给蝴蝶结。
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(end = GREETING_BOW_CLEARANCE),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = dateSubline(now, totalMl, goalMl),
            color = spec.greetingSubColor,
            // 日期副行（§10.4）：小字距让信息行更安静，与上方问候语拉开层次。
            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.3.sp),
            textAlign = TextAlign.Center,
        )
    }
}
