package com.awakedw.core.domain

import com.awakedw.core.common.AppClock
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.WaterRecord
import kotlinx.coroutines.flow.first

/** 打卡结果：[Logged] 携带本次写入的杯量记录与「是否触发当日首次达标庆祝」。 */
sealed interface LogResult {
    data class Logged(
        val record: WaterRecord,
        val celebrated: Boolean,
    ) : LogResult
}

/** 记一笔喝水：按设置写入一杯；若本次使当日总量首次达到/超过目标，持久化 celebrated_day_key 并返回 celebrated=true。 */
class LogWaterUseCase(
    private val water: WaterRepository,
    private val prefs: UserPreferencesRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(): LogResult {
        val settings = prefs.settings.first()
        val record = water.addCup(settings.cupMl)
        val totalAfterLog = water.todayStats().totalMl
        val reachedGoal = totalAfterLog >= settings.goalMl
        val notCelebratedYet = prefs.celebratedDayKey() != record.dayKeyLocal
        val celebrated = reachedGoal && notCelebratedYet
        if (celebrated) {
            prefs.markCelebrated(record.dayKeyLocal)
        }
        return LogResult.Logged(record = record, celebrated = celebrated)
    }
}
