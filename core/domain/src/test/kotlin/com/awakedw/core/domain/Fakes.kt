package com.awakedw.core.domain

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
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** 固定假钟：[ms] 可手动推进，时区固定 Asia/Shanghai（与既往任务一致）。 */
class FakeClock(var ms: Long) : AppClock {
    override fun nowEpochMs(): Long = ms

    override fun zone(): ZoneId = ZoneId.of("Asia/Shanghai")

    fun setAtLocal(
        hour: Int,
        minute: Int,
    ) {
        val zoned = Instant.ofEpochMilli(ms).atZone(zone())
        ms = zoned.toLocalDate().atTime(hour, minute).atZone(zone()).toInstant().toEpochMilli()
    }
}

/** 内存版水记录仓储：changes 用 replay=1 的 SharedFlow 供给，与 Room「首值即当前态」语义对齐。 */
class FakeWaterRepository(
    private val clock: FakeClock,
) : WaterRepository {
    private var nextId = 1L
    private val totalsByDay = mutableMapOf<String, Int>()
    private val recorded = mutableListOf<WaterRecord>()
    private val _changes = MutableSharedFlow<Unit>(replay = 1)

    override val changes: Flow<Unit> = _changes

    init {
        _changes.tryEmit(Unit)
    }

    fun emitChange() {
        _changes.tryEmit(Unit)
    }

    /** 直接铺历史数据：指定某日总毫升数。 */
    fun seedTotal(
        dayKey: String,
        totalMl: Int,
    ) {
        totalsByDay[dayKey] = totalMl
    }

    private fun currentDayKey(): String = clock.nowEpochMs().toDayKey(clock.zone())

    override suspend fun addCup(amountMl: Int): WaterRecord {
        val record =
            WaterRecord(id = nextId++, amountMl = amountMl, drankAtEpochMs = clock.nowEpochMs(), dayKeyLocal = currentDayKey())
        recorded += record
        totalsByDay[currentDayKey()] = (totalsByDay[currentDayKey()] ?: 0) + amountMl
        emitChange()
        return record
    }

    override suspend fun todayStats(): DailyStats {
        val today = recorded.filter { it.dayKeyLocal == currentDayKey() }
        return DailyStats(totalMl = totalsByDay[currentDayKey()] ?: 0, cupCount = today.size, avgIntervalMin = avgIntervalOf(today))
    }

    override suspend fun weekBars(daysBack: Int): List<WeekBar> {
        require(daysBack > 0)
        val today = Instant.ofEpochMilli(clock.nowEpochMs()).atZone(clock.zone()).toLocalDate()
        return ((daysBack - 1) downTo 0).map { offset ->
            val key = today.minusDays(offset.toLong()).toString()
            WeekBar(dayKey = key, totalMl = totalsByDay[key] ?: 0)
        }
    }

    override suspend fun todayRecords(): List<WaterRecord> = recorded.filter { it.dayKeyLocal == currentDayKey() }

    private fun avgIntervalOf(records: List<WaterRecord>): Int? {
        if (records.size < 2) return null
        val spanMs = (records.last().drankAtEpochMs - records.first().drankAtEpochMs).toDouble()
        return (spanMs / (records.size - 1) / 60_000.0).roundToInt()
    }
}

/** 内存版用户设置仓储：settings 为 StateFlow 快照流，测试内经接口方法改写即触发下游。 */
class FakeUserPreferencesRepository(
    initial: UserSettings = UserSettings(),
) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    private var celebrated: String? = null
    private var doneOnboarding = false

    override val settings: Flow<UserSettings> = state

    override suspend fun setGoalMl(v: Int) = state.update { it.copy(goalMl = v) }

    override suspend fun setCupMl(v: Int) = state.update { it.copy(cupMl = v) }

    override suspend fun setWindow(
        startMin: Int,
        endMin: Int,
    ) = state.update { it.copy(windowStartMin = startMin, windowEndMin = endMin) }

    override suspend fun setIntervalMin(v: Int) = state.update { it.copy(intervalMin = v) }

    override suspend fun setRemindersEnabled(v: Boolean) = state.update { it.copy(remindersEnabled = v) }

    override suspend fun setThemeChoice(v: ThemeChoice) = state.update { it.copy(themeChoice = v) }

    override suspend fun markCelebrated(dayKey: String) {
        celebrated = dayKey
    }

    override suspend fun celebratedDayKey(): String? = celebrated

    override suspend fun markOnboardingDone() {
        doneOnboarding = true
    }

    override suspend fun onboardingDone(): Boolean = doneOnboarding

    // —— v0.2 画廊与音效（内存版，仅满足契约加宽） ——

    private val sound = MutableStateFlow(true)

    override val soundEnabled: Flow<Boolean> = sound

    override suspend fun setSoundEnabled(v: Boolean) {
        sound.value = v
    }
}
