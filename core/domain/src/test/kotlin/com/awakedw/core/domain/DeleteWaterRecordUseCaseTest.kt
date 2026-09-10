package com.awakedw.core.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 记录纠错的领域语义：记错的一杯要能自己收回，同时**不能**被用来刷出额外反馈。
 *
 * 关键约束是庆祝标记不参与回滚——否则「撤回 → 再记」的循环可以反复触发达标庆祝，
 * 让一个纯确认性的反馈退化成可刷的奖励。
 */
class DeleteWaterRecordUseCaseTest {
    private lateinit var clock: FakeClock
    private lateinit var water: FakeWaterRepository
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var logWater: LogWaterUseCase
    private lateinit var deleteWater: DeleteWaterRecordUseCase

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    @Before
    fun setUp() {
        clock = FakeClock(ZonedDateTime.of(2026, 8, 27, 10, 0, 0, 0, zone).toInstant().toEpochMilli())
        water = FakeWaterRepository(clock)
        prefs = FakeUserPreferencesRepository()
        logWater = LogWaterUseCase(water, prefs, clock)
        deleteWater = DeleteWaterRecordUseCase(water)
    }

    private suspend fun logCup(amountMl: Int = 250) {
        clock.ms += 1_000
        logWater(amountMl)
    }

    @Test
    fun `撤回今日最后一杯后总量与杯数同步回落`() =
        runBlocking {
            logCup()
            logCup()
            logCup()
            assertEquals(750, water.todayStats().totalMl)

            assertNotNull(deleteWater.revertLatestToday())

            val stats = water.todayStats()
            assertEquals(500, stats.totalMl)
            assertEquals(2, stats.cupCount)
        }

    @Test
    fun `撤回的是最后记下的那一杯并把它交还给调用方`() =
        runBlocking {
            logCup(100)
            logCup(250)
            logCup(380)

            val removed = deleteWater.revertLatestToday()

            // 交还的记录用于「删掉了哪一杯」的确认，杯量必须是被删那一笔而不是设置值。
            assertNotNull(removed)
            assertEquals(380, removed?.amountMl)
            assertEquals(listOf(100, 250), water.todayRecords().map { it.amountMl })
        }

    @Test
    fun `今日没有记录时撤回不写入也不报错`() =
        runBlocking {
            assertNull(deleteWater.revertLatestToday())
            assertEquals(0, water.todayStats().cupCount)
            assertTrue(water.todayRecords().isEmpty())
        }

    @Test
    fun `按id删除只影响那一笔_重复删除静默忽略`() =
        runBlocking {
            logCup(100)
            logCup(250)
            val middle = water.todayRecords().first()

            deleteWater(middle.id)
            assertEquals(listOf(250), water.todayRecords().map { it.amountMl })

            // 同一个 id 再删一次：静默忽略，不影响剩余记录。
            deleteWater(middle.id)
            assertEquals(listOf(250), water.todayRecords().map { it.amountMl })
        }

    @Test
    fun `撤回不回滚庆祝标记_撤回后再达标不重复庆祝`() =
        runBlocking {
            prefs.setGoalMl(500)

            val first = logWater(250) as LogResult.Logged
            assertEquals(false, first.celebrated)

            val reached = logWater(250) as LogResult.Logged
            assertEquals(true, reached.celebrated)
            val celebratedKey = prefs.celebratedDayKey()
            assertEquals("2026-08-27", celebratedKey)

            // 撤回达标那一杯——标记保留，避免「撤回 → 再记」循环刷出庆祝。
            deleteWater.revertLatestToday()
            assertEquals(celebratedKey, prefs.celebratedDayKey())

            clock.ms += 1_000
            val again = logWater(250) as LogResult.Logged
            assertEquals(false, again.celebrated)
        }

    @Test
    fun `撤回后未达标时后续记录仍可正常写入`() =
        runBlocking {
            prefs.setGoalMl(500)
            logWater(250)
            logWater(250)
            deleteWater.revertLatestToday()

            val stats = water.todayStats()
            assertEquals(250, stats.totalMl)
            assertEquals(1, stats.cupCount)
            assertNull(stats.avgIntervalMin)
        }
}
