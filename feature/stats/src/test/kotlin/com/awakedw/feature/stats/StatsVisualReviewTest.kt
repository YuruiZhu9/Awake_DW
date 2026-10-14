package com.awakedw.feature.stats

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.LocalUiTimeMillis
import com.awakedw.core.designsystem.art.loadAssetBitmap
import com.awakedw.core.designsystem.lolita.themeArtworkOf
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import com.awakedw.core.testsupport.VisualBaseline
import com.awakedw.feature.stats.components.DELETE_HINT_TEXT
import com.awakedw.feature.stats.components.EMPTY_TIMELINE_TEXT
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * 统计页视觉基线（2.7.0 起，roadmap P1）：截图与随仓基线比对，意外视觉漂移在构建期被绊住。
 * 时段氛围经 [LocalUiTimeMillis] 钉定、素材预载消除解码竞态、减少动态全静态——
 * 跨日跨时段逐像素近一致。基线是回归绊网，真机仍是最终视觉闸门。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsVisualReviewTest {
    @get:Rule
    val rule = createComposeRule()

    /** 与首页基线同值的钉定时刻：时段氛围恒落在同一确定点。 */
    private val pinnedUiTimeMillis = 1_760_000_000_000L

    @Test
    fun `populated stats stay readable and each day exposes its real amount`() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        lateinit var view: View
        val theme = mutableStateOf(ThemeId.EMERALD)
        val font = mutableStateOf(1f)
        val state =
            StatsUiState(
                StatsBadges(1250, 5, "约 1 小时"),
                (2..8).map { WeekBar("2026-09-0$it", if (it == 8) 1250 else it * 250) },
                1600,
                (1..5).map { WaterRecord(it.toLong(), 250, 1_788_854_400_000L + it * 3_600_000L, "2026-09-08") },
                todayKey = "2026-09-08",
            )
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalUiTimeMillis provides pinnedUiTimeMillis) {
                AwakeTheme(theme.value) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, font.value)) {
                        StatsContent(state)
                    }
                }
            }
        }
        for (id in ThemeId.entries) {
            // 每轮只预载当前主题素材（2.7.0）：主图 + 中景层就位，截图不再与异步解码赛跑——
            // 全部预载会互相驱逐（首轮 stats/night 46% 漂移），漏预载中景则留下负载相关竞态
            // （Release 变体并行负载下 theme 级偶发漂移的根因）。
            preloadThemeAssets(id)
            rule.runOnIdle { theme.value = id }
            rule.onNodeWithText("统计").performScrollTo().assertIsDisplayed()
            capture(view, id.name.lowercase())
        }
        rule.runOnIdle { font.value = 1.5f }
        // 周记录列表（2.5.0 去图表化）：今天一行翻在最上，大字体下不丢内容。
        rule.onNodeWithContentDescription("今天，1250ml").performScrollTo().assertIsDisplayed()
        capture(view, "large-week")
        rule.onNodeWithText("今日记录 · 5 次").performScrollTo().assertIsDisplayed()
        capture(view, "large-records")
    }

    /**
     * 达标标记与时间线删除说明行：两者都只在实际有数据/达标时出现，
     * 上面的填充态（1250 / 1600）既不达标、时间线又落在折叠线以下，一张图都拍不到。
     * 这里把目标设为已达成，并把时间线滚进视口，留下证据。
     */
    @Test
    fun `reached target is marked and the timeline discloses its delete gesture`() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        preloadThemeAssets(ThemeId.EMERALD)
        lateinit var view: View
        val state =
            StatsUiState(
                StatsBadges(1800, 6, "约 56 分钟"),
                (2..8).map { WeekBar("2026-09-0$it", if (it == 8) 1800 else 0) },
                1600,
                (1..3).map { WaterRecord(it.toLong(), 600, 1_788_854_400_000L + it * 3_600_000L, "2026-09-08") },
            )
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalUiTimeMillis provides pinnedUiTimeMillis) {
                AwakeTheme(ThemeId.EMERALD) { StatsContent(state) }
            }
        }

        rule.onNodeWithText("已达成").assertExists()
        capture(view, "target-met")

        rule.onNodeWithText(DELETE_HINT_TEXT).performScrollTo().assertIsDisplayed()
        capture(view, "timeline-delete")
    }

    @Test
    fun `empty stats show a plain empty state without an imaginary record`() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        preloadThemeAssets(ThemeId.CLERIC)
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalUiTimeMillis provides pinnedUiTimeMillis) {
                AwakeTheme(ThemeId.CLERIC) { StatsContent(StatsUiState()) }
            }
        }
        rule.onNodeWithText(EMPTY_TIMELINE_TEXT).performScrollTo().assertIsDisplayed()
        capture(view, "empty")
    }

    /** 预载单主题的主图与中景层素材（2.7.0）：页面像素的全部异步来源就此关闭。 */
    private fun preloadThemeAssets(id: ThemeId) {
        val artwork = themeArtworkOf(id)
        runBlocking {
            loadAssetBitmap(RuntimeEnvironment.getApplication(), artwork.asset)
            artwork.midgroundAsset?.let { loadAssetBitmap(RuntimeEnvironment.getApplication(), it) }
        }
    }

    /** 截图入库并与视觉基线比对（2.7.0）：意外视觉漂移在构建期被绊住，更新走 `AWAKE_VISUAL_BASELINE=update`。 */
    private fun capture(
        view: View,
        label: String,
    ) {
        VisualBaseline.verify(
            module = "stats",
            label = label,
            settle = {
                rule.waitForIdle()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_000L))
                rule.waitForIdle()
            },
        ) {
            lateinit var bitmap: Bitmap
            rule.runOnIdle {
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
            }
            bitmap
        }
    }
}
