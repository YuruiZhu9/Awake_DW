package com.awakedw.core.designsystem

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** UI 时间缝（2.7.0 视觉基线）：注入固定时刻后，界面读数原样返回该时刻。 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class UiTimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `注入固定时刻后界面时间原样返回该值`() {
        val expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(1_760_000_000_000L), ZoneId.systemDefault())
        var observed: LocalDateTime? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalUiTimeMillis provides 1_760_000_000_000L) {
                observed = rememberUiNow()
            }
        }
        assertEquals(expected, observed)
    }
}
