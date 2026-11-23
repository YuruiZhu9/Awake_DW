package com.awakedw.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberUiNow
import java.time.DayOfWeek
import java.time.LocalDateTime

/** Stable Chinese weekday labels for the date masthead. */
fun editorialDateLabel(dateTime: LocalDateTime): String {
    val weekday =
        when (dateTime.dayOfWeek) {
            DayOfWeek.MONDAY -> "星期一"
            DayOfWeek.TUESDAY -> "星期二"
            DayOfWeek.WEDNESDAY -> "星期三"
            DayOfWeek.THURSDAY -> "星期四"
            DayOfWeek.FRIDAY -> "星期五"
            DayOfWeek.SATURDAY -> "星期六"
            DayOfWeek.SUNDAY -> "星期日"
        }
    return "${dateTime.monthValue}月${dateTime.dayOfMonth}日 · $weekday"
}

/** 页码 folio：每日一页，页序用月内日期；只做版面家具，不进读屏主叙事。 */
fun editorialFolio(dateTime: LocalDateTime): String = "%02d".format(dateTime.dayOfMonth)

/** 页脚页码文案：与 folio 同值，保持书上「第 NN 页」的读法。 */
fun editorialPageLine(folio: String): String = "第 $folio 页"

/**
 * 统计/设置页的页眉：日期行 + 斜体 folio + 通栏发丝线 + 衬线标题与副题。
 *
 * 与首页刊头同一套版式语法（日期 → 线 → 衬线大标题），只把问候换成页面标题；
 * 版式开销约 80dp，两页都可滚动，不参与首页的一屏门禁。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun EditorialPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    now: LocalDateTime = rememberUiNow(),
) {
    val spec = currentThemeSpec()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = editorialDateLabel(now),
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = editorialFolio(now),
                color = spec.greetingSubColor.copy(alpha = 0.85f),
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                    ),
                modifier = Modifier.alignByBaseline().testTag("editorial-folio"),
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(spec.greetingSubColor.copy(alpha = 0.40f)),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = title,
            color = spec.greetingColor,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = spec.greetingSubColor,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** 页脚：一条发丝线 +「刊名 · 栏目 / 第 NN 页」，是纸的收口，不是状态栏。 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun EditorialPageFooter(
    folio: String,
    modifier: Modifier = Modifier,
    imprint: String = "AWAKE · 每日一页",
) {
    val spec = currentThemeSpec()
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(spec.greetingSubColor.copy(alpha = 0.30f)),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = imprint,
                color = spec.greetingSubColor.copy(alpha = 0.72f),
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 1.6.sp,
                    ),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = editorialPageLine(folio),
                color = spec.greetingSubColor.copy(alpha = 0.72f),
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 1.6.sp,
                    ),
            )
        }
    }
}
