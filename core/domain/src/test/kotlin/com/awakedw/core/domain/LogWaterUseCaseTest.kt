package com.awakedw.core.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class LogWaterUseCaseTest {
    private lateinit var clock: FakeClock
    private lateinit var water: FakeWaterRepository
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var useCase: LogWaterUseCase

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val todayKey = "2026-08-27"

    @Before
    fun setUp() {
        clock = FakeClock(ZonedDateTime.of(2026, 8, 27, 10, 0, 0, 0, zone).toInstant().toEpochMilli())
        water = FakeWaterRepository(clock)
        prefs = FakeUserPreferencesRepository()
        useCase = LogWaterUseCase(water, prefs, clock)
    }

    @Test
    fun `当日首次达标才庆祝_第二次不再庆祝`() =
        runBlocking {
            prefs.setGoalMl(250) // 单杯即可达标
            prefs.setCupMl(250)

            val first = useCase() as LogResult.Logged
            val record1 = first.record

            assertEquals(250, record1.amountMl)
            assertEquals(clock.nowEpochMs(), record1.drankAtEpochMs)
            assertEquals(todayKey, record1.dayKeyLocal)
            assertEquals(true, first.celebrated)
            assertEquals(todayKey, prefs.celebratedDayKey())

            val second = useCase() as LogResult.Logged
            assertEquals(false, second.celebrated)
        }

    @Test
    fun `未达标不庆祝且不写庆祝键`() =
        runBlocking {
            val result = useCase() as LogResult.Logged

            assertEquals(false, result.celebrated)
            assertNull(prefs.celebratedDayKey())
        }

    @Test
    fun `次日重新达标可再次庆祝`() =
        runBlocking {
            prefs.setGoalMl(250) // 单杯即达标

            assertEquals(true, (useCase() as LogResult.Logged).celebrated)

            clock.ms += 24 * 60 * 60_000L // 次日同时刻（东八区无夏令时，直接推一天）
            val nextDayResult = useCase() as LogResult.Logged

            assertEquals("2026-08-28", nextDayResult.record.dayKeyLocal)
            assertEquals(true, nextDayResult.celebrated)
            assertEquals("2026-08-28", prefs.celebratedDayKey())
        }
}
