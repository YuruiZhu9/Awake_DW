package com.awakedw.feature.stats

import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.TimeSlot
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
 * 2.1.0 两节新内容（本月热力 + 时段节律）在填充态下的整页渲染测试：
 * 旧的可视化回归用旧四参构造，monthCells/rhythm 全为默认空值——新组件从未被渲染过，
 * 真机打开统计页闪退而测试全绿的盲区正是这里。此测试把两节全部填满数据。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsArchiveRenderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `filled month heatmap and rhythm bars render without crashing`() {
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
                rhythm =
                    listOf(
                        RhythmSlice(TimeSlot.MORNING, 500),
                        RhythmSlice(TimeSlot.DAY, 250),
                        RhythmSlice(TimeSlot.EVENING, 500),
                    ),
                rhythmSummary = "近七日时段分布：早 500ml，白天 250ml，晚 500ml",
            )
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) { StatsContent(state) }
        }
        rule.onNodeWithText("时段节律 · 近七日").performScrollTo()
        rule.onNodeWithText("早").performScrollTo()
        // 热力格数字在合并树里不可见（容器整体一条摘要），分节标题出现即证明热力图组装成功。
        rule.onNodeWithText("9月").performScrollTo()
    }
}
