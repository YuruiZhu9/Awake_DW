package com.awakedw.core.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class GetStreakUseCaseTest {
    private lateinit var clock: FakeClock
    private lateinit var water: FakeWaterRepository
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var useCase: GetStreakUseCase

    private fun dayKeyOf(monthDay: String): String = "2026-08-$monthDay"

    @Before
    fun setUp() {
        clock = FakeClock(ZonedDateTime.of(2026, 8, 27, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli())
        water = FakeWaterRepository(clock)
        prefs = FakeUserPreferencesRepository()
        useCase = GetStreakUseCase(water, prefs)
    }

    @Test
    fun `昨天与今天均达标_连胜为2`() =
        runBlocking {
            water.seedTotal(dayKeyOf("26"), 2000)
            water.seedTotal(dayKeyOf("27"), 2000)

            assertEquals(2, useCase())
        }

    @Test
    fun `昨天缺席_仅今天达标_连胜为1`() =
        runBlocking {
            water.seedTotal(dayKeyOf("25"), 2000) // 更早的连续天应在「昨缺」处断链
            water.seedTotal(dayKeyOf("27"), 2000)

            assertEquals(1, useCase())
        }

    @Test
    fun `今天尚未达标不算断忌_此前连续两天连胜为2`() =
        runBlocking {
            water.seedTotal(dayKeyOf("25"), 2000)
            water.seedTotal(dayKeyOf("26"), 2000)
            water.seedTotal(dayKeyOf("27"), 100)

            assertEquals(2, useCase())
        }

    @Test
    fun `目标上调后历史总量不达新标_连胜归零`() =
        runBlocking {
            water.seedTotal(dayKeyOf("26"), 1700)
            water.seedTotal(dayKeyOf("27"), 1700)
            assertEquals(2, useCase())

            prefs.setGoalMl(1800)

            assertEquals(0, useCase())
        }
}
