package com.awakedw.core.domain

import com.awakedw.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 6 行表驱动覆盖 brief 全部分支；期望值均为独立推得的当日时刻 epoch，另补一条「恰压窗口终点」的边界例。 */
class NextReminderCalculatorTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val day: LocalDate = LocalDate.of(2026, 8, 27)

    private fun epochAt(
        hour: Int,
        minute: Int,
    ): Long = ZonedDateTime.of(day, LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()

    private fun settings(
        startMin: Int = 480,
        endMin: Int = 1350,
        intervalMin: Int = 90,
        remindersEnabled: Boolean = true,
    ) = UserSettings(
        windowStartMin = startMin,
        windowEndMin = endMin,
        intervalMin = intervalMin,
        remindersEnabled = remindersEnabled,
    )

    private data class Case(
        val name: String,
        val settings: UserSettings,
        val nowEpoch: Long,
        val achievedToday: Boolean,
        val expectedFire: Long?,
    )

    @Test
    fun `表驱动_禁用或已达成或未开窗或窗口内加间隔或越界或改参六例`() {
        val cases =
            listOf(
                Case("禁用即不排程", settings(remindersEnabled = false), epochAt(12, 30), false, null),
                Case("已达成今日不再打扰", settings(), epochAt(12, 30), true, null),
                Case("07:00未开窗_今天08:00整点火", settings(), epochAt(7, 0), false, epochAt(8, 0)),
                Case("08:50在窗内_now基准加90分", settings(), epochAt(8, 50), false, epochAt(10, 20)),
                Case("21:40再过90分越22:30界_返回null", settings(), epochAt(21, 40), false, null),
                Case(
                    "改参数后按新窗口与新间隔重算",
                    settings(startMin = 570, endMin = 1320, intervalMin = 120),
                    epochAt(19, 30),
                    false,
                    epochAt(21, 30),
                ),
            )

        cases.forEach { c ->
            val fire = NextReminderCalculator.nextFire(c.settings, FakeClock(c.nowEpoch), c.achievedToday)
            assertEquals(c.name, c.expectedFire, fire)
        }
    }

    @Test
    fun `恰落窗口终点不算越界_仍排程`() {
        // windowEnd=21:00、interval=45：now 20:15 + 45min 正好压线 21:00，视为有效。
        val settings = settings(endMin = 21 * 60, intervalMin = 45)
        assertEquals(epochAt(21, 0), NextReminderCalculator.nextFire(settings, FakeClock(epochAt(20, 15)), false))
    }
}
