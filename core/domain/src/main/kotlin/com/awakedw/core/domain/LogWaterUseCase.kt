package com.awakedw.core.domain

import com.awakedw.core.common.AppClock
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.WaterRecord
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 打卡结果：[Logged] 携带本次写入的杯量记录、「是否触发当日首次达标庆祝」与记完之后的当日总量。 */
sealed interface LogResult {
    data class Logged(
        val record: WaterRecord,
        val celebrated: Boolean,
        /**
         * 记这一杯之后的当日总量。
         *
         * 本值本就在实现里算出来了（判达标要用），此前算完即弃。
         * 之所以对外暴露：通知动作「记一杯」的调用方不在应用内，看不到进度环，
         * 回执要能直接交代「今天共多少」才算完整——否则使用者点完还得打开应用才知道。
         */
        val totalAfterLog: Int,
    ) : LogResult
}

/** 记一笔喝水：[amountMl] 缺省按设置写一杯，传快捷量（小口 / 一杯半）则写该量；
 * 若本次使当日总量首次达到/超过目标，持久化 celebrated_day_key 并返回 celebrated=true。 */
class LogWaterUseCase
    @Inject
    constructor(
        private val water: WaterRepository,
        private val prefs: UserPreferencesRepository,
        private val clock: AppClock,
    ) {
        suspend operator fun invoke(amountMl: Int? = null): LogResult {
            val settings = prefs.settings.first()
            val record = water.addCup(amountMl ?: settings.cupMl)
            val totalAfterLog = water.todayStats().totalMl
            val reachedGoal = totalAfterLog >= settings.goalMl
            val notCelebratedYet = prefs.celebratedDayKey() != record.dayKeyLocal
            val celebrated = reachedGoal && notCelebratedYet
            if (celebrated) {
                prefs.markCelebrated(record.dayKeyLocal)
            }
            return LogResult.Logged(record = record, celebrated = celebrated, totalAfterLog = totalAfterLog)
        }
    }
