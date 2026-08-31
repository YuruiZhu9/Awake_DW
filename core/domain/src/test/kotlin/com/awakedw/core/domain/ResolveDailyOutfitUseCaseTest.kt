package com.awakedw.core.domain

import com.awakedw.core.model.OutfitCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ResolveDailyOutfitUseCaseTest {
    private lateinit var clock: FakeClock
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var useCase: ResolveDailyOutfitUseCase

    private val dayKey = "2026-08-31"

    @Before
    fun setUp() {
        clock = FakeClock(ZonedDateTime.of(2026, 8, 31, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli())
        prefs = FakeUserPreferencesRepository()
        useCase = ResolveDailyOutfitUseCase(prefs, clock)
    }

    @Test
    fun `同池同key两次挑选结果一致`() {
        val pool = OutfitCatalog.all.filter { it.id in setOf("dress_00", "dress_01") }

        val first = pickForDay(dayKey, pool)
        val second = pickForDay(dayKey, pool)

        assertEquals(first, second)
    }

    @Test
    fun `钉选优先_无视随机直接返回钉选件`() =
        runBlocking {
            prefs.markOutfitsUnlocked(listOf("dress_00", "dress_01"))
            prefs.setPinnedOutfit("museum_02") // 钉选件不在已解锁池里也应优先

            val outfit = useCase()

            assertEquals("museum_02", outfit.id)
        }

    @Test
    fun `已解锁池为空_回退dress00`() =
        runBlocking {
            val outfit = useCase()

            assertEquals("dress_00", outfit.id)
        }

    @Test
    fun `同日第二次调用不重挑_读当日落库记录`() =
        runBlocking {
            prefs.markOutfitsUnlocked(listOf("dress_00", "dress_01"))
            val first = useCase()
            assertEquals(dayKey to first.id, prefs.dailyOutfit())

            // 池此后扩张也不影响今天：当日记录命中优先于随机
            prefs.markOutfitsUnlocked(listOf("dress_02"))

            val second = useCase()

            assertEquals(first, second)
        }
}
