package com.awakedw.core.data

import androidx.room.Room
import app.cash.turbine.test
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.data.db.AwakeDb
import com.awakedw.core.data.repo.RoomWaterRepository
import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.ZoneId
import java.time.ZonedDateTime

/** 固定假钟：[ms] 可手动推进，时区固定 Asia/Shanghai。 */
class FakeClock(var ms: Long) : AppClock {
    override fun nowEpochMs(): Long = ms

    override fun zone(): ZoneId = ZoneId.of("Asia/Shanghai")
}

@RunWith(RobolectricTestRunner::class)
class RoomWaterRepositoryTest {
    private lateinit var db: AwakeDb
    private lateinit var clock: FakeClock
    private lateinit var repo: RoomWaterRepository

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    /** 当天 10:00（本地），可选偏移分钟数。 */
    private fun tenOclock(minuteOffset: Long = 0L): Long {
        val at = ZonedDateTime.of(2026, 8, 20, 10, 0, 0, 0, zone).plusMinutes(minuteOffset)
        return at.toInstant().toEpochMilli()
    }

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db =
            Room.inMemoryDatabaseBuilder(context, AwakeDb::class.java)
                .allowMainThreadQueries()
                .build()
        clock = FakeClock(tenOclock())
        repo = RoomWaterRepository(db.waterRecordDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `同日三杯_间隔60与30分钟_avg45`() {
        runBlocking {
            repo.addCup(250)

            clock.ms = tenOclock(60L)
            repo.addCup(250)

            clock.ms = tenOclock(90L)
            val third = repo.addCup(250)

            // total=750 杯数=3；last-first=90min，(90/2)=45min
            assertEquals(DailyStats(totalMl = 750, cupCount = 3, avgIntervalMin = 45), repo.todayStats())

            // 记录按时间升序
            val records = repo.todayRecords()
            assertEquals(3, records.size)
            assertTrue(records.zipWithNext().all { (a, b) -> a.drankAtEpochMs <= b.drankAtEpochMs })
            assertEquals(third, records.last())

            // 周条目：恰 7 天且以今天收尾（750），其余补 0
            val bars = repo.weekBars()
            assertEquals(7, bars.size)
            assertEquals(WeekBar(tenOclock().toDayKey(zone), 750), bars.last())
            assertTrue(bars.dropLast(1).all { it.totalMl == 0 })
        }
    }

    @Test
    fun `空库统计为0且无平均间隔`() {
        runBlocking {
            assertEquals(DailyStats(totalMl = 0, cupCount = 0, avgIntervalMin = null), repo.todayStats())

            val bars = repo.weekBars()
            assertEquals(7, bars.size)
            assertEquals(tenOclock().toDayKey(zone), bars.last().dayKey)
            assertTrue(bars.all { it.totalMl == 0 })
        }
    }

    @Test
    fun `任一写入都令变更流再发射一次`() {
        runBlocking {
            repo.changes.test {
                assertEquals(Unit, awaitItem()) // 订阅首值：当前态

                repo.addCup(250)
                assertEquals(Unit, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
