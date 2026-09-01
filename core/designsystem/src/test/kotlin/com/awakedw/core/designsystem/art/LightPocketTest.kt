package com.awakedw.core.designsystem.art

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 光袋（moodboard §2 光·遇手法）：
 *
 * - 组合不崩溃：默认色（主题 haloColor）与显式色两条签名路径都挂载即过（呼吸帧循环下 waitForIdle 不挂起）；
 * - 简报逐字呼吸参数用纯函数直断言：pocketAlpha 三点 0f→0.06f、0.5f→0.14f、1f→0.06f，
 *   并以 0.25/0.75 中点锁定「线性往返」（呼吸不是闪烁——频率克制、无高频脉动）；
 * - 周期对齐：1.5s 单程 ×2 = 3s 完整呼吸周期，与 Task 9 裁定的 CatFigure BREATH_LEG_MS 同值。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class LightPocketTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---------- 组合不崩溃 ----------

    @Test
    fun `默认主题色光袋组合不崩溃`() {
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.EMERALD) {
                LightPocket(modifier = Modifier.size(96.dp))
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `显式色光袋组合不崩溃`() {
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.NIGHT) {
                LightPocket(modifier = Modifier.size(160.dp), color = Color.White)
            }
        }
        composeRule.waitForIdle()
    }

    // ---------- 简报逐字参数：纯函数直断言 ----------

    @Test
    fun `呼吸alpha三点与简报逐字一致`() {
        assertEquals(0.06f, pocketAlpha(0f))
        assertEquals(0.14f, pocketAlpha(0.5f))
        assertEquals(0.06f, pocketAlpha(1f))
    }

    @Test
    fun `呼吸alpha线性往返无高频脉动`() {
        // 前半程线性升、后半程线性降：中点两侧对称回到半程值（中点允许 1 ulp 内的浮点差）。
        assertEquals(0.10f, pocketAlpha(0.25f), 1e-6f)
        assertEquals(0.10f, pocketAlpha(0.75f), 1e-6f)
        // 越界相位收敛到端点（呼吸到头即折返，不越幅）。
        assertEquals(0.06f, pocketAlpha(-0.5f))
        assertEquals(0.06f, pocketAlpha(1.5f))
    }

    @Test
    fun `呼吸周期对齐3秒完整周期且与猫呼吸同裁定`() {
        // 1.5s 单程（升）×2（降）= 3s 完整呼吸周期；单程时长与 Task 9 CatFigure 同值同裁定。
        assertEquals(1500, POCKET_LEG_MS)
        assertEquals(BREATH_LEG_MS, POCKET_LEG_MS)
    }
}
