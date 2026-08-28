package com.awakedw.core.domain

import com.awakedw.core.common.AppClock
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.TimeSlot
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

/** 时段 → 主题映射：清晨草莓（轻快起势）、日间翡翠（清爽专注）、夜晚焦糖（温暖收尾）。 */
private fun themeFor(slot: TimeSlot): ThemeId =
    when (slot) {
        TimeSlot.MORNING -> ThemeId.STRAWBERRY
        TimeSlot.DAY -> ThemeId.EMERALD
        TimeSlot.EVENING -> ThemeId.CARAMEL
    }

/**
 * 主题解析：FIXED_* 直接映射同名 [ThemeId]；FOLLOW_TIME 按当前时段映射，
 * 每 [resamplePeriodMs] 重读一次时钟，跨过时段边界（如上午 11 点）后自动流出新值。
 */
class ResolveThemeUseCase(
    private val prefs: UserPreferencesRepository,
    private val clock: AppClock,
    private val resamplePeriodMs: Long = RESAMPLE_PERIOD_MS,
) {
    /** Dagger 注入入口：生产以默认重采样周期委托主构造器（JSR-330 不识别 Kotlin 缺省参数）。 */
    @Inject
    constructor(
        prefs: UserPreferencesRepository,
        clock: AppClock,
    ) : this(
        prefs,
        clock,
        RESAMPLE_PERIOD_MS,
    )

    operator fun invoke(): Flow<ThemeId> =
        prefs.settings
            .distinctUntilChanged()
            .flatMapLatest { settings -> resolveAsFlow(settings.themeChoice) }
            .distinctUntilChanged()

    private fun resolveAsFlow(choice: ThemeChoice): Flow<ThemeId> =
        when (choice) {
            ThemeChoice.FOLLOW_TIME -> followTimeSlots()
            ThemeChoice.FIXED_EMERALD -> flowOf(ThemeId.EMERALD)
            ThemeChoice.FIXED_STRAWBERRY -> flowOf(ThemeId.STRAWBERRY)
            ThemeChoice.FIXED_CARAMEL -> flowOf(ThemeId.CARAMEL)
        }

    /** 周期重读时钟的时段流：没有可订阅的外部时间事件源，轻量轮询是最简可靠的刷新方式。 */
    private fun followTimeSlots(): Flow<ThemeId> =
        flow {
            while (currentCoroutineContext().isActive) {
                emit(currentFollowTimeTheme())
                delay(resamplePeriodMs)
            }
        }.distinctUntilChanged()

    private fun currentFollowTimeTheme(): ThemeId {
        val hour = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.nowEpochMs()), clock.zone()).hour
        return themeFor(TimeSlots.slotOfHour(hour))
    }

    companion object {
        /** 跟随时段的重采样周期；生产为每分钟一次，测试可在虚拟时间轴上精确推进。 */
        const val RESAMPLE_PERIOD_MS = 60_000L
    }
}
