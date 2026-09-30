package com.awakedw.feature.stats

import java.time.LocalDate

/**
 * 统计页纯换算（2.5.0 去图表化后只保留「记录」形态的文案与读法：
 * 周合计、达标天数、日期读法、细轨语义与周记录行的日期标签）。
 * 柱高/目标线/分布条等图表换算已随柱状图与节律条撤除。
 */
object StatsMath {
    /**
     * 近七日合计的展示文案：满 1L 用一位小数的升，不足 1L 保留毫升——
     * 与环心、徽章的「毫升叙事」衔接，避免近两千毫升挤成四位数。
     */
    fun weekTotalLabel(totalMl: Int): String =
        if (totalMl >= 1000) {
            val liters = totalMl / 1000.0
            val text = String.format(java.util.Locale.ROOT, "%.1f", liters)
            if (text.endsWith(".0")) "${text.dropLast(2)} L" else "$text L"
        } else {
            "$totalMl ml"
        }

    /** 近七日达标天数展示：`N/7 天`；窗口恒为七天（含今天），分母固定。 */
    fun weekMetDaysLabel(
        values: List<Int>,
        goalMl: Int,
    ): String = "${metGoal(values, goalMl).count { it }}/7 天"

    /** 逐日「是否达标」标记：达标柱用主题 primary，其余用轨道色。 */
    private fun metGoal(
        values: List<Int>,
        goalMl: Int,
    ): List<Boolean> = values.map { it >= goalMl }

    /** 选中列读数的日期段（0.9.2）：`2026-09-08` → 「9月8日」；解析失败原样返回，不造日期。 */
    fun dayReadout(dayKey: String): String =
        runCatching {
            LocalDate.parse(dayKey).let { "${it.monthValue}月${it.dayOfMonth}日" }
        }.getOrDefault(dayKey)

    /**
     * 周记录行的日期标签（2.5.0）：今天、昨天，其余转「M月D日」——
     * 记录列表用称呼而不是坐标，读起来像翻日记而不是看图表。
     */
    fun weekRowLabel(
        dayKey: String,
        todayKey: String,
    ): String =
        when {
            dayKey == todayKey -> "今天"
            runCatching { LocalDate.parse(todayKey).minusDays(1).toString() == dayKey }.getOrDefault(false) -> "昨天"
            else -> dayReadout(dayKey)
        }

    /** 细轨进度的语义读法（0.9.2）：达标即陈述达标；未达标给整数百分比（整除向下取整，不满不虚报）。 */
    fun todayProgressLabel(
        totalMl: Int,
        goalMl: Int,
    ): String {
        val goal = goalMl.coerceAtLeast(1)
        return if (totalMl >= goal) "今日已达标" else "今日进度 ${totalMl * 100 / goal}%"
    }
}

/** 月历热力的一个格子；[isFuture] 的格子尚未到来，只占位不上色。 */
data class MonthCell(
    val dayNumber: Int,
    val dayKey: String,
    val totalMl: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/**
 * 统计月历的纯换算（2.1.0 引入，2.5.0 收敛为三档「记录」语义）。
 * 只做事实陈述，不引入成就、连续或奖励语义（D10）。
 */
object ArchiveMath {
    /**
     * 本月网格：周一首行，月初的空位用 `null` 占位；
     * 1 日到月末逐日生成，未来日期 [MonthCell.isFuture] 为真（数据只到今天）。
     * [todayKey] 解析失败返回空列表——不造日期。
     */
    fun monthCells(
        todayKey: String,
        totalsByDay: Map<String, Int>,
    ): List<MonthCell?> {
        val today = runCatching { LocalDate.parse(todayKey) }.getOrNull() ?: return emptyList()
        val leadBlanks = today.dayOfWeek.value - 1 // 周一 = 1 → 无偏移；周日 = 7 → 六个空位
        val cells = MutableList<MonthCell?>(leadBlanks) { null }
        for (day in 1..today.lengthOfMonth()) {
            val date = today.withDayOfMonth(day)
            val key = date.toString()
            cells +=
                MonthCell(
                    dayNumber = day,
                    dayKey = key,
                    totalMl = totalsByDay[key] ?: 0,
                    isToday = day == today.dayOfMonth,
                    isFuture = day > today.dayOfMonth,
                )
        }
        return cells
    }

    /**
     * 热力档位（2.5.0 三档）：0 未饮、1 有记录、2 达标——
     * 五档深浅的梯度感是图表语言，三档只回答「这天喝没喝、够没够」。
     */
    fun heatLevel(
        totalMl: Int,
        goalMl: Int,
    ): Int =
        when {
            totalMl <= 0 -> 0
            totalMl >= goalMl -> 2
            else -> 1
        }

    /** 月历的语义摘要： TalkBack 读一句事实，不逐格走 31 个数字。 */
    fun monthSummary(
        cells: List<MonthCell?>,
        goalMl: Int,
    ): String {
        val real = cells.filterNotNull().filterNot { it.isFuture }
        val recorded = real.count { it.totalMl > 0 }
        val met = real.count { it.totalMl >= goalMl }
        return "本月有记录 $recorded 天，达标 $met 天"
    }

    /** 月标题：`2026-09-28` → 「9月」；解析失败返回空串，由调用方决定退路。 */
    fun monthTitle(todayKey: String): String = runCatching { "${LocalDate.parse(todayKey).monthValue}月" }.getOrDefault("")
}
