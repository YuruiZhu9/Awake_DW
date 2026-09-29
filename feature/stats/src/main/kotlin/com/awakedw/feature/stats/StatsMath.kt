package com.awakedw.feature.stats

import com.awakedw.core.common.TimeSlots
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.WaterRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 统计页周柱状图的纯几何换算（设计规格 §3.3 第 2 条）。
 *
 * 柱与目标线共用同一把刻度尺：以「周内最大柱量与目标量中的较大者」为刻度顶，
 * 映射到图表高 × [SCALE_FRACTION] 的刻度带内——最大柱至多顶到 0.86 倍图高，
 * 目标线无论高低都落在带内、与柱身保持真实比例。
 * 量值为 0 的柱归一到 0f 高度，由绘制层画成基线圆点（「这天还没喝」的温柔占位）。
 */
object StatsMath {
    /** Normalize each delayed column so the last frame is the real value, not a shortened bar. */
    fun columnGrowth(
        progress: Float,
        index: Int,
    ): Float {
        val delay = (index.coerceAtLeast(0) * 0.06f).coerceAtMost(0.80f)
        return ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
    }

    /** 柱与目标线共用的刻度带上限：占图表高的比例。 */
    const val SCALE_FRACTION = 0.86f

    /**
     * 归一化柱高（与 [chartHeight] 同单位）：[values] 逐日映射为自基线起算的高度；
     * 0 值返回 0f，绘制层据此改画基线圆点。
     */
    fun barHeights(
        values: List<Int>,
        goalMl: Int,
        chartHeight: Float,
    ): List<Float> {
        val scaleTop = bandHeight(chartHeight) / scaleMaxOf(values, goalMl)
        return values.map { value -> value * scaleTop }
    }

    /** 目标虚线的 y 坐标（自顶部计，与 [chartHeight] 同单位）：目标量按与柱同刻度映射。 */
    fun goalLineY(
        goalMl: Int,
        values: List<Int>,
        chartHeight: Float,
    ): Float = chartHeight - goalMl * bandHeight(chartHeight) / scaleMaxOf(values, goalMl)

    /** 逐日「是否达标」标记：达标柱用主题 primary，其余用轨道色。 */
    fun metGoal(
        values: List<Int>,
        goalMl: Int,
    ): List<Boolean> = values.map { it >= goalMl }

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

    /** 柱底标注：今天列写「今」，其余列写当月几号。 */
    fun columnLabels(
        dayKeys: List<String>,
        todayKey: String,
    ): List<String> =
        dayKeys.map { key ->
            if (key == todayKey) "今" else LocalDate.parse(key).dayOfMonth.toString()
        }

    /** 选中列读数的日期段（0.9.2）：`2026-09-08` → 「9月8日」；解析失败原样返回，不造日期。 */
    fun dayReadout(dayKey: String): String =
        runCatching {
            LocalDate.parse(dayKey).let { "${it.monthValue}月${it.dayOfMonth}日" }
        }.getOrDefault(dayKey)

    /** 细轨进度的语义读法（0.9.2）：达标即陈述达标；未达标给整数百分比（整除向下取整，不满不虚报）。 */
    fun todayProgressLabel(
        totalMl: Int,
        goalMl: Int,
    ): String {
        val goal = goalMl.coerceAtLeast(1)
        return if (totalMl >= goal) "今日已达标" else "今日进度 ${totalMl * 100 / goal}%"
    }

    /** 刻度顶取「最大柱、目标」中的较大者；全零周退化为目标量本身。 */
    private fun scaleMaxOf(
        values: List<Int>,
        goalMl: Int,
    ): Int = (values.maxOrNull() ?: 0).coerceAtLeast(goalMl).coerceAtLeast(1)

    private fun bandHeight(chartHeight: Float): Float = chartHeight * SCALE_FRACTION
}

/** 月历热力的一个格子；[isFuture] 的格子尚未到来，只占位不上色。 */
data class MonthCell(
    val dayNumber: Int,
    val dayKey: String,
    val totalMl: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/** 时段节律的一段：某时段在统计窗口内的合计毫升数（零也有位置，图例保持稳定）。 */
data class RhythmSlice(
    val slot: TimeSlot,
    val totalMl: Int,
)

/** 年度信纸的事实（2.3.0）：全年总量、杯数与最常饮水的时段；零记录年不写信。 */
data class YearLetterData(
    val totalMl: Int,
    val cupCount: Int,
    val topSlot: TimeSlot,
)

/**
 * 统计档案的纯换算（2.1.0）：月历热力网格、热力档位、时段节律聚合与两条摘要文案。
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

    /** 热力档位 0–4：0 为未饮；达标即最高档；中间按距目标的三等分就近落档。 */
    fun heatLevel(
        totalMl: Int,
        goalMl: Int,
    ): Int =
        when {
            totalMl <= 0 -> 0
            totalMl >= goalMl -> 4
            totalMl * 3 >= goalMl * 2 -> 3
            totalMl * 3 >= goalMl -> 2
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

    /**
     * 近七日时段节律：记录按 `TimeSlots`（早/白天/晚）分桶合计，顺序固定、零段保留。
     * 时段语言与心意文案库、问候语完全一致，不新增第四个时段。
     */
    fun rhythmOf(
        records: List<WaterRecord>,
        zone: ZoneId,
    ): List<RhythmSlice> {
        val totals = mutableMapOf<TimeSlot, Int>()
        records.forEach { record ->
            val hour = Instant.ofEpochMilli(record.drankAtEpochMs).atZone(zone).hour
            val slot = TimeSlots.slotOfHour(hour)
            totals[slot] = (totals[slot] ?: 0) + record.amountMl
        }
        return listOf(TimeSlot.MORNING, TimeSlot.DAY, TimeSlot.EVENING).map { slot ->
            RhythmSlice(slot = slot, totalMl = totals[slot] ?: 0)
        }
    }

    /** 节律图例的单字标签：现代白话，与堆叠条的三段一一对应。 */
    fun slotLabel(slot: TimeSlot): String =
        when (slot) {
            TimeSlot.MORNING -> "早"
            TimeSlot.DAY -> "白天"
            TimeSlot.EVENING -> "晚"
        }

    /** 堆叠条的语义读法：一段一句事实，供 TalkBack 一次读完。 */
    fun rhythmSummary(slices: List<RhythmSlice>): String =
        slices.joinToString(separator = "，") { "${slotLabel(it.slot)} ${it.totalMl}ml" }
            .let { "近七日时段分布：$it" }

    /**
     * 年度信纸的纯聚合（2.3.0）：把一年窗口内的记录合计为总量与杯数，
     * 时段沿用 [TimeSlots] 三分桶，并列时取固定顺序（早→白天→晚）的第一个。
     * [records] 为空返回 null——当年还没有任何记录时不写信。
     */
    fun yearLetter(
        records: List<WaterRecord>,
        zone: ZoneId,
    ): YearLetterData? {
        if (records.isEmpty()) return null
        val slotTotals = mutableMapOf<TimeSlot, Int>()
        records.forEach { record ->
            val hour = Instant.ofEpochMilli(record.drankAtEpochMs).atZone(zone).hour
            val slot = TimeSlots.slotOfHour(hour)
            slotTotals[slot] = (slotTotals[slot] ?: 0) + record.amountMl
        }
        val topSlot =
            listOf(TimeSlot.MORNING, TimeSlot.DAY, TimeSlot.EVENING)
                .maxByOrNull { slotTotals[it] ?: 0 } ?: TimeSlot.MORNING
        return YearLetterData(
            totalMl = records.sumOf { it.amountMl },
            cupCount = records.size,
            topSlot = topSlot,
        )
    }

    /** 信纸正文两行（2.3.0）：只陈述事实，现代白话；总量沿用周合计的升/毫升双格式。 */
    fun letterLines(
        data: YearLetterData,
        year: Int,
    ): List<String> =
        listOf(
            "$year 年，共记下 ${StatsMath.weekTotalLabel(data.totalMl)}、${data.cupCount} 杯。",
            "喝得最多的时段是${slotLabel(data.topSlot)}。",
        )
}
