package com.awakedw.core.domain

import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ResolveThemeUseCaseTest {
    private lateinit var clock: FakeClock
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var useCase: ResolveThemeUseCase

    @Before
    fun setUp() {
        clock = FakeClock(ZonedDateTime.of(2026, 8, 27, 10, 59, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli())
        prefs = FakeUserPreferencesRepository()
        useCase = ResolveThemeUseCase(prefs, clock)
    }

    @Test
    fun `FOLLOW_TIME_拨过11点后流出新值EMERALD`() =
        runTest {
            val received = mutableListOf<ThemeId>()
            backgroundScope.launch {
                useCase().collect {
                    received += it
                    if (received.size >= 2) this.coroutineContext.cancel()
                }
            }

            // 初值：10:59 属 MORNING（<11），随后时钟跨过 11:00 应切换为 DAY 的映射值 EMERALD。
            runCurrent()
            assertEquals(listOf(ThemeId.STRAWBERRY), received)

            clock.setAtLocal(11, 0)
            advanceTimeBy(RESAMPLE_MS + 1)

            assertEquals(listOf(ThemeId.STRAWBERRY, ThemeId.EMERALD), received)
        }

    @Test
    fun `FIXED_主题直接映射`() =
        runTest {
            listOf(
                ThemeChoice.FIXED_EMERALD to ThemeId.EMERALD,
                ThemeChoice.FIXED_STRAWBERRY to ThemeId.STRAWBERRY,
                ThemeChoice.FIXED_CARAMEL to ThemeId.CARAMEL,
                ThemeChoice.FIXED_NIGHT to ThemeId.NIGHT,
                ThemeChoice.FIXED_LAVENDER to ThemeId.LAVENDER,
            ).forEach { (choice, expected) ->
                prefs.setThemeChoice(choice)
                assertEquals(expected, useCase().first())
            }
        }

    @Test
    fun `FOLLOW_TIME_深夜边界_22点切墨青_06点回草莓_傍晚保持焦糖`() =
        runTest {
            suspend fun themeAt(
                hour: Int,
                minute: Int = 0,
            ): ThemeId {
                clock.setAtLocal(hour, minute)
                return useCase().first()
            }

            // 18–21 点的 EVENING 槽维持焦糖奶茶。
            assertEquals(ThemeId.CARAMEL, themeAt(18, 0))
            assertEquals(ThemeId.CARAMEL, themeAt(21, 59))
            // 22:00 起切入深夜墨青，横跨子夜直到 05:59。
            assertEquals(ThemeId.NIGHT, themeAt(22, 0))
            assertEquals(ThemeId.NIGHT, themeAt(23, 59))
            assertEquals(ThemeId.NIGHT, themeAt(0, 0))
            assertEquals(ThemeId.NIGHT, themeAt(5, 59))
            // 06:00 回到清晨草莓。
            assertEquals(ThemeId.STRAWBERRY, themeAt(6, 0))
        }

    companion object {
        /** 与实现的默认重采样周期一致：测试据此推进虚拟时间。 */
        const val RESAMPLE_MS = 60_000L
    }
}
