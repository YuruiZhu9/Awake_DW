package com.awakedw.feature.home

import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.UserSettings
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** 固定假钟：[ms] 可手动推进，时区固定 Asia/Shanghai（与域层测试一致）。 */
class FakeClock(
    var ms: Long,
) : AppClock {
    override fun nowEpochMs(): Long = ms

    override fun zone(): ZoneId = ZoneId.of("Asia/Shanghai")
}

/**
 * 内存版水记录仓储：changes 用 replay=1 的 SharedFlow 供给，
 * 与 Room「首值即当前态」语义对齐；平均间隔算法复刻 RoomWaterRepository。
 */
class FakeWaterRepository(
    private val clock: FakeClock,
) : WaterRepository {
    var addCount: Int = 0
        private set

    private var nextId = 1L
    private val records = mutableListOf<WaterRecord>()
    private val _changes = MutableSharedFlow<Unit>(replay = 1)

    override val changes: Flow<Unit> = _changes

    init {
        _changes.tryEmit(Unit)
    }

    /** 直接铺今日历史：首杯落在本地 10:00，[gapsMin] 依次给出与上一杯的间隔分钟（杯数 = gaps+1）。 */
    fun seedToday(
        vararg gapsMin: Int,
        amountMl: Int = DEFAULT_CUP_ML,
    ) {
        val zone = clock.zone()
        var t = Instant.ofEpochMilli(clock.ms).atZone(zone).toLocalDate().atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        records += WaterRecord(id = nextId++, amountMl = amountMl, drankAtEpochMs = t, dayKeyLocal = t.toDayKey(zone))
        gapsMin.forEach { gap ->
            t += gap * 60_000L
            records += WaterRecord(id = nextId++, amountMl = amountMl, drankAtEpochMs = t, dayKeyLocal = t.toDayKey(zone))
        }
        _changes.tryEmit(Unit)
    }

    override suspend fun addCup(amountMl: Int): WaterRecord {
        addCount += 1
        val record =
            WaterRecord(
                id = nextId++,
                amountMl = amountMl,
                drankAtEpochMs = clock.nowEpochMs(),
                dayKeyLocal = clock.nowEpochMs().toDayKey(clock.zone()),
            )
        records += record
        _changes.tryEmit(Unit)
        return record
    }

    override suspend fun todayStats(): DailyStats {
        val today = records.filter { it.dayKeyLocal == currentDayKey() }
        return DailyStats(
            totalMl = today.sumOf { it.amountMl },
            cupCount = today.size,
            avgIntervalMin = avgIntervalMinOf(today),
            lastDrankAtEpochMs = today.maxOfOrNull { it.drankAtEpochMs },
        )
    }

    override suspend fun weekBars(daysBack: Int): List<WeekBar> {
        require(daysBack > 0)
        val today = Instant.ofEpochMilli(clock.nowEpochMs()).atZone(clock.zone()).toLocalDate()
        return (daysBack - 1 downTo 0).map { offset ->
            val key = today.minusDays(offset.toLong()).toDayKey()
            WeekBar(dayKey = key, totalMl = if (offset == 0) todayTotal() else 0)
        }
    }

    override suspend fun todayRecords(): List<WaterRecord> =
        records.filter { it.dayKeyLocal == currentDayKey() }.sortedBy { it.drankAtEpochMs }

    private fun currentDayKey(): String = clock.nowEpochMs().toDayKey(clock.zone())

    private fun todayTotal(): Int = records.filter { it.dayKeyLocal == currentDayKey() }.sumOf { it.amountMl }

    /** 杯数 <2 时无平均间隔可言；否则取首尾跨度 /(n-1) 折算分钟并四舍五入。 */
    private fun avgIntervalMinOf(list: List<WaterRecord>): Int? {
        if (list.size < 2) return null
        val spanMs = (list.last().drankAtEpochMs - list.first().drankAtEpochMs).toDouble()
        return (spanMs / (list.size - 1) / 60_000.0).roundToInt()
    }

    private companion object {
        const val DEFAULT_CUP_ML = 250
    }
}

/** 内存版用户设置仓储：setter 即改 StateFlow，celebrated_day_key 记在内存。 */
class FakePrefsRepository(
    initial: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
) : UserPreferencesRepository {
    private val _settings = MutableStateFlow(initial)

    override val settings = _settings

    var celebratedKeyValue: String? = null
        private set

    override suspend fun setGoalMl(v: Int) {
        _settings.value = _settings.value.copy(goalMl = v)
    }

    override suspend fun setCupMl(v: Int) {
        _settings.value = _settings.value.copy(cupMl = v)
    }

    override suspend fun setWindow(
        startMin: Int,
        endMin: Int,
    ) {
        _settings.value = _settings.value.copy(windowStartMin = startMin, windowEndMin = endMin)
    }

    override suspend fun setIntervalMin(v: Int) {
        _settings.value = _settings.value.copy(intervalMin = v)
    }

    override suspend fun setRemindersEnabled(v: Boolean) {
        _settings.value = _settings.value.copy(remindersEnabled = v)
    }

    override suspend fun setThemeChoice(v: ThemeChoice) {
        _settings.value = _settings.value.copy(themeChoice = v)
    }

    override suspend fun markCelebrated(dayKey: String) {
        celebratedKeyValue = dayKey
    }

    override suspend fun celebratedDayKey(): String? = celebratedKeyValue

    override suspend fun markOnboardingDone() = Unit

    override suspend fun onboardingDone(): Boolean = true

    // —— v0.2 画廊与音效（内存版，仅满足契约加宽） ——
    private val unlocked = MutableStateFlow(emptySet<String>())
    private val pinned = MutableStateFlow<String?>(null)
    private var daily: Pair<String, String>? = null
    private val sound = MutableStateFlow(true)

    override val unlockedOutfits: Flow<Set<String>> = unlocked

    override suspend fun markOutfitsUnlocked(ids: Collection<String>) {
        unlocked.value = unlocked.value + ids.toSet()
    }

    override val pinnedOutfitId: Flow<String?> = pinned

    override suspend fun setPinnedOutfit(id: String?) {
        pinned.value = id
    }

    override suspend fun dailyOutfit(): Pair<String, String>? = daily

    override suspend fun setDailyOutfit(
        dayKey: String,
        outfitId: String,
    ) {
        daily = dayKey to outfitId
    }

    override val soundEnabled: Flow<Boolean> = sound

    override suspend fun setSoundEnabled(v: Boolean) {
        sound.value = v
    }
}

/** 固定文案库：按时段返回固定短句，并记录被询问过的时段供断言。 */
class FakeCopyLibraryRepository : CopyLibraryRepository {
    val requestedSlots = mutableListOf<TimeSlot>()

    private val _library =
        MutableStateFlow(
            CopyLibrary(morning = listOf("早安短句"), day = listOf("日间短句"), evening = listOf("晚安短句")),
        )

    override val library = _library

    override suspend fun randomFor(
        slot: TimeSlot,
        avoidRecent: Int,
    ): String {
        requestedSlots += slot
        return _library.value.groupOf(slot).first()
    }

    override suspend fun upsert(
        slot: TimeSlot,
        index: Int,
        text: String,
    ) = Unit

    override suspend fun delete(
        slot: TimeSlot,
        index: Int,
    ) = Unit

    override suspend fun resetToDefaults() = Unit
}
