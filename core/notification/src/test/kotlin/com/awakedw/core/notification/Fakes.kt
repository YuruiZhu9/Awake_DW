package com.awakedw.core.notification

import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.UserSettings
import com.awakedw.core.model.WaterRecord
import com.awakedw.core.model.WeekBar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 测试统一时区。 */
val TEST_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

/** 测试统一「今天」：2026-08-27。 */
private val TEST_DAY = ZonedDateTime.of(2026, 8, 27, 10, 0, 0, 0, TEST_ZONE).toLocalDate()

/** 当天本地 [hour]:[minute] 的 epoch 毫秒。 */
fun atLocal(
    hour: Int,
    minute: Int,
): Long = ZonedDateTime.of(TEST_DAY, LocalTime.of(hour, minute), TEST_ZONE).toInstant().toEpochMilli()

/** 固定假钟：[ms] 可手动推进，时区固定 Asia/Shanghai（与既往任务一致）。 */
class FakeClock(var ms: Long) : AppClock {
    override fun nowEpochMs(): Long = ms

    override fun zone(): ZoneId = TEST_ZONE
}

/** 内存版水记录仓储：只覆盖调度链路消费的成员，周条目不参与（返回空表）。 */
class FakeWaterRepository(
    private val clock: FakeClock,
) : WaterRepository {
    val records = mutableListOf<WaterRecord>()

    override val changes: Flow<Unit> = MutableStateFlow(Unit)

    override suspend fun addCup(amountMl: Int): WaterRecord {
        val record =
            WaterRecord(
                id = records.size + 1L,
                amountMl = amountMl,
                drankAtEpochMs = clock.nowEpochMs(),
                dayKeyLocal = clock.nowEpochMs().toDayKey(clock.zone()),
            )
        records += record
        return record
    }

    override suspend fun todayStats(): DailyStats =
        DailyStats(totalMl = records.sumOf { it.amountMl }, cupCount = records.size, avgIntervalMin = null)

    override suspend fun weekBars(daysBack: Int): List<WeekBar> = emptyList()

    override suspend fun todayRecords(): List<WaterRecord> = records.toList()
}

/** 内存版用户设置仓储：settings 为 StateFlow 快照流，经接口方法改写即触发下游。 */
class FakeUserPreferencesRepository(
    initial: UserSettings = UserSettings(),
) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    private var celebrated: String? = null
    private var doneOnboarding = false
    private val sound = MutableStateFlow(true)

    override val settings: Flow<UserSettings> = state

    override suspend fun setGoalMl(v: Int) = state.update { it.copy(goalMl = v) }

    override suspend fun setCupMl(v: Int) = state.update { it.copy(cupMl = v) }

    override suspend fun setWindow(
        startMin: Int,
        endMin: Int,
    ) = state.update { it.copy(windowStartMin = startMin, windowEndMin = endMin) }

    override suspend fun setIntervalMin(v: Int) = state.update { it.copy(intervalMin = v) }

    override suspend fun setRemindersEnabled(v: Boolean) = state.update { it.copy(remindersEnabled = v) }

    override suspend fun setThemeChoice(v: com.awakedw.core.model.ThemeChoice) = state.update { it.copy(themeChoice = v) }

    override suspend fun markCelebrated(dayKey: String) {
        celebrated = dayKey
    }

    override suspend fun celebratedDayKey(): String? = celebrated

    override suspend fun markOnboardingDone() {
        doneOnboarding = true
    }

    override suspend fun onboardingDone(): Boolean = doneOnboarding

    // —— v0.2 画廊与音效（内存版，仅满足契约加宽） ——

    override val soundEnabled: Flow<Boolean> = sound

    override suspend fun setSoundEnabled(v: Boolean) {
        sound.value = v
    }
}

/** 固定文案仓储：按时段返回确定句子并记录抽取时段，测试可直接断言通知正文。 */
class FakeCopyLibraryRepository : CopyLibraryRepository {
    /** randomFor 被调用的时段序列（断言「按当前时段取文案」）。 */
    val picks = mutableListOf<TimeSlot>()

    private val state = MutableStateFlow(CopyLibrary(morning = emptyList(), day = emptyList(), evening = emptyList()))

    override val library: Flow<CopyLibrary> = state

    override suspend fun randomFor(
        slot: TimeSlot,
        avoidRecent: Int,
    ): String {
        picks += slot
        return when (slot) {
            TimeSlot.MORNING -> "早一句"
            TimeSlot.DAY -> "午一句"
            TimeSlot.EVENING -> "晚一句"
        }
    }

    override suspend fun randomCatLine(
        slot: TimeSlot,
        avoidRecent: Int,
    ): String = "喵一句"

    override suspend fun upsert(
        slot: TimeSlot,
        index: Int,
        text: String,
    ) {
        // 调度链路不消费：保持空实现。
    }

    override suspend fun delete(
        slot: TimeSlot,
        index: Int,
    ) {
        // 调度链路不消费：保持空实现。
    }

    override suspend fun resetToDefaults() {
        // 调度链路不消费：保持空实现。
    }
}

/** 测试假体集中器：@Before 重置一次，测试图 @Provides 与测试断言共享同一批实例。 */
object TestFakes {
    lateinit var clock: FakeClock
        private set
    lateinit var water: FakeWaterRepository
        private set
    lateinit var prefs: FakeUserPreferencesRepository
        private set
    lateinit var copies: FakeCopyLibraryRepository
        private set

    /** 每个测试开始时重建全套假体：默认设置、当天 10:00。 */
    fun reset(settings: UserSettings = UserSettings()) {
        val clock = FakeClock(atLocal(10, 0))
        this.clock = clock
        water = FakeWaterRepository(clock)
        prefs = FakeUserPreferencesRepository(settings)
        copies = FakeCopyLibraryRepository()
    }
}
