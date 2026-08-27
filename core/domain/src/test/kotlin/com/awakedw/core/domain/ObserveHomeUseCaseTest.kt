package com.awakedw.core.domain

import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ObserveHomeUseCaseTest {
    private lateinit var clock: FakeClock
    private lateinit var water: FakeWaterRepository
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var useCase: ObserveHomeUseCase

    private val yesterdayKey = "2026-08-26"

    @Before
    fun setUp() {
        clock = FakeClock(ZonedDateTime.of(2026, 8, 27, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli())
        water = FakeWaterRepository(clock)
        prefs = FakeUserPreferencesRepository()
        // 注入以同一组 fake 构建的主题解析器，使 12 点固定解析为 DAY→EMERALD。
        useCase = ObserveHomeUseCase(water, prefs, ResolveThemeUseCase(prefs, clock))
    }

    @Test
    fun `首快照齐活且设置与水库变化均触发重算`() =
        runTest {
            water.seedTotal(yesterdayKey, 2000)
            water.addCup(250) // 今日统计 (250,1,null)；changes 虽先发射但被 replay=1 重放给订阅者

            val snapshots = mutableListOf<HomeSnapshot>()
            backgroundScope.launch {
                useCase().collect {
                    snapshots += it
                    if (snapshots.size >= 3) this.coroutineContext.cancel()
                }
            }

            // 快照一：昨天达标（连胜 1：今天未达标不计入）、12 点主题 EMERALD。
            runCurrent()
            prefs.setGoalMl(240) // 目标下调即时生效：今日已达标 → 连胜升为 2
            runCurrent()
            water.emitChange() // 水库变化触发器独立可见：产出同内容新快照
            runCurrent()

            val expectedFirst =
                HomeSnapshot(
                    stats = DailyStats(totalMl = 250, cupCount = 1, avgIntervalMin = null),
                    goalMl = 1600,
                    cupMl = 250,
                    themeId = ThemeId.EMERALD,
                    streakDays = 1,
                )
            val expectedAfterGoalLowered =
                expectedFirst.copy(goalMl = 240, streakDays = 2)

            assertEquals(listOf(expectedFirst, expectedAfterGoalLowered, expectedAfterGoalLowered), snapshots)
        }

    @Test
    fun `快照字段组装正确_含连胜对目标的实时响应`() =
        runTest {
            water.seedTotal(yesterdayKey, 2000)
            water.addCup(250)

            val expectedFirst =
                HomeSnapshot(
                    stats = DailyStats(totalMl = 250, cupCount = 1, avgIntervalMin = null),
                    goalMl = 1600,
                    cupMl = 250,
                    themeId = ThemeId.EMERALD,
                    streakDays = 1,
                )

            assertEquals(expectedFirst, useCase().first())
        }
}
