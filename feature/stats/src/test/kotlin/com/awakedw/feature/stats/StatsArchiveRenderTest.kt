package com.awakedw.feature.stats

import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 2.5.0 去图表化后的统计页填充态整页渲染测试：周记录列表 + 本月日历都要真的组装出来。
 * 此测试的前身（2.1.0）曾暴露「新组件零渲染覆盖 → 真机必崩」的盲区，形态更新后继续守这一层。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsArchiveRenderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `周记录列表与月历填充渲染不崩溃`() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        val monthTotals =
            mapOf(
                "2026-09-01" to 800,
                "2026-09-15" to 1600,
                "2026-09-29" to 1250,
            )
        val state =
            StatsUiState(
                StatsBadges(1250, 5, "约 1 小时"),
                (23..29).map { WeekBar("2026-09-$it", if (it == 29) 1250 else it * 40) },
                1600,
                (1..5).map { WaterRecord(it.toLong(), 250, 1_788_854_400_000L + it * 3_600_000L, "2026-09-29") },
                monthCells = ArchiveMath.monthCells("2026-09-29", monthTotals),
                monthLabel = "9月",
                monthSummary = "本月有记录 3 天，达标 1 天",
                todayKey = "2026-09-29",
            )
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) { StatsContent(state) }
        }
        // 周记录列表：今天翻在最上（仓储契约 bars 末列为今天），昨天次之。
        rule.onNodeWithContentDescription("今天，1250ml").performScrollTo()
        rule.onNodeWithContentDescription("昨天，1120ml").performScrollTo()
        // 月历：分节标题出现即证明日历组装成功（格子数字在合并树里不可见，容器整体一条摘要）。
        rule.onNodeWithText("9月").performScrollTo()
    }
}
