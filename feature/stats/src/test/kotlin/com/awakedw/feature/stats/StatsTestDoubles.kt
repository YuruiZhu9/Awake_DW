package com.awakedw.feature.stats

import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** 固定假钟：[ms] 可手动推进，时区固定 Asia/Shanghai（与既往任务测试一致）。 */
class FakeClock(
    var ms: Long,
) : AppClock {
    override fun nowEpochMs(): Long = ms

    override fun zone(): ZoneId = ZoneId.of("Asia/Shanghai")
}

/**
 * 内存版水记录仓储：changes 用 replay=1 的 SharedFlow 供给，与 Room「首值即当前态」语义对齐；
 * 每日总量与今日记录分别可铺，平均间隔算法复刻 RoomWaterRepository。
 */
class FakeWaterRepository(
    private val clock: FakeClock,
) : WaterRepository {
    private var nextId = 1L
    private val records = mutableListOf<WaterRecord>()
    private val totalsByDay = mutableMapOf<String, Int>()
    private val _changes = MutableSharedFlow<Unit>(replay = 1)

    override val changes: Flow<Unit> = _changes

    init {
        _changes.tryEmit(Unit)
    }

    /** 直接铺某日总毫升数（铺历史连胜用）。 */
    fun seedTotal(
        dayKey: String,
        totalMl: Int,
    ) {
        totalsByDay[dayKey] = totalMl
        _changes.tryEmit(Unit)
    }

    /** 铺今日记录：首杯落在本地 10:00，[gapsMin] 依次给出与上一杯的间隔分钟（杯数 = gaps+1）。 */
    fun seedToday(
        vararg gapsMin: Int,
        amountMl: Int = DEFAULT_CUP_ML,
    ) {
        val zone = clock.zone()
        var t = Instant.ofEpochMilli(clock.ms).atZone(zone).toLocalDate().atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        appendRecord(t, amountMl)
        gapsMin.forEach { gap ->
            t += gap * 60_000L
            appendRecord(t, amountMl)
        }
        _changes.tryEmit(Unit)
    }

    override suspend fun addCup(amountMl: Int): WaterRecord {
        val record = appendRecord(clock.nowEpochMs(), amountMl)
        _changes.tryEmit(Unit)
        return record
    }

    override suspend fun todayStats(): DailyStats {
        val today = recordsOf(currentDayKey())
        return DailyStats(
            totalMl = totalsByDay[currentDayKey()] ?: 0,
            cupCount = today.size,
            avgIntervalMin = avgIntervalMinOf(today),
        )
    }

    override suspend fun weekBars(daysBack: Int): List<WeekBar> {
        require(daysBack > 0)
        val today = Instant.ofEpochMilli(clock.nowEpochMs()).atZone(clock.zone()).toLocalDate()
        return ((daysBack - 1) downTo 0).map { offset ->
            val key = today.minusDays(offset.toLong()).toString()
            WeekBar(dayKey = key, totalMl = totalsByDay[key] ?: 0)
        }
    }

    override suspend fun todayRecords(): List<WaterRecord> = recordsOf(currentDayKey()).sortedBy { it.drankAtEpochMs }

    private fun appendRecord(
        atEpochMs: Long,
        amountMl: Int,
    ): WaterRecord {
        val dayKey = atEpochMs.toDayKey(clock.zone())
        totalsByDay[dayKey] = (totalsByDay[dayKey] ?: 0) + amountMl
        return WaterRecord(
            id = nextId++,
            amountMl = amountMl,
            drankAtEpochMs = atEpochMs,
            dayKeyLocal = dayKey,
        ).also { records += it }
    }

    private fun currentDayKey(): String = clock.nowEpochMs().toDayKey(clock.zone())

    private fun recordsOf(dayKey: String): List<WaterRecord> = records.filter { it.dayKeyLocal == dayKey }

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

/** 内存版用户设置仓储：settings 为 StateFlow，setter 即改即发射。 */
class FakePrefsRepository(
    initial: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
) : UserPreferencesRepository {
    private val _settings = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = _settings

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

    override suspend fun markCelebrated(dayKey: String) = Unit

    override suspend fun celebratedDayKey(): String? = null

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
