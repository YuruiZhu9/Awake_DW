package com.awakedw.feature.settings

import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** 内存版用户设置仓储：setter 即改 StateFlow，并记录各 setter 的调用轨迹供直通断言。 */
class FakePrefsRepository(
    initial: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
    private val onboardingDoneValue: Boolean = true,
) : UserPreferencesRepository {
    private val _settings = MutableStateFlow(initial)

    override val settings = _settings

    /** setter 调用轨迹：「goal=2000」式的短记录；非法入仓调用不应出现在这里。 */
    val calls = mutableListOf<String>()

    override suspend fun setGoalMl(v: Int) {
        calls += "goal=$v"
        _settings.value = _settings.value.copy(goalMl = v)
    }

    override suspend fun setCupMl(v: Int) {
        calls += "cup=$v"
        _settings.value = _settings.value.copy(cupMl = v)
    }

    override suspend fun setWindow(
        startMin: Int,
        endMin: Int,
    ) {
        calls += "window=$startMin-$endMin"
        _settings.value = _settings.value.copy(windowStartMin = startMin, windowEndMin = endMin)
    }

    override suspend fun setIntervalMin(v: Int) {
        calls += "interval=$v"
        _settings.value = _settings.value.copy(intervalMin = v)
    }

    override suspend fun setRemindersEnabled(v: Boolean) {
        calls += "reminders=$v"
        _settings.value = _settings.value.copy(remindersEnabled = v)
    }

    override suspend fun setThemeChoice(v: ThemeChoice) {
        calls += "theme=$v"
        _settings.value = _settings.value.copy(themeChoice = v)
    }

    override suspend fun markCelebrated(dayKey: String) = Unit

    override suspend fun celebratedDayKey(): String? = null

    override suspend fun markOnboardingDone() = Unit

    override suspend fun onboardingDone(): Boolean = onboardingDoneValue
}

/** 内存版文案库仓储：增删改即时落到 StateFlow，供「编辑 → UiState 回灌」断言。 */
class FakeCopyLibraryRepository(
    initial: CopyLibrary =
        CopyLibrary(
            morning = listOf("早安，喝水啦", "晨光和第一杯水"),
            day = listOf("午后补一杯"),
            evening = listOf("晚安前最后一杯"),
        ),
) : CopyLibraryRepository {
    private val _library = MutableStateFlow(initial)

    override val library = _library

    var resetCount: Int = 0
        private set

    override suspend fun randomFor(
        slot: TimeSlot,
        avoidRecent: Int,
    ): String = _library.value.groupOf(slot).first()

    override suspend fun upsert(
        slot: TimeSlot,
        index: Int,
        text: String,
    ) {
        val group = _library.value.groupOf(slot).toMutableList()
        if (index < group.size) {
            group[index] = text
        } else {
            group += text
        }
        _library.value = _library.value.withGroup(slot, group)
    }

    override suspend fun delete(
        slot: TimeSlot,
        index: Int,
    ) {
        val group = _library.value.groupOf(slot).toMutableList()
        if (index in group.indices) {
            group.removeAt(index)
            _library.value = _library.value.withGroup(slot, group)
        }
    }

    override suspend fun resetToDefaults() {
        resetCount += 1
        _library.value =
            CopyLibrary(
                morning = listOf("默认早安"),
                day = listOf("默认午安"),
                evening = listOf("默认晚安"),
            )
    }

    private fun CopyLibrary.withGroup(
        slot: TimeSlot,
        group: List<String>,
    ): CopyLibrary =
        when (slot) {
            TimeSlot.MORNING -> copy(morning = group)
            TimeSlot.DAY -> copy(day = group)
            TimeSlot.EVENING -> copy(evening = group)
        }
}
