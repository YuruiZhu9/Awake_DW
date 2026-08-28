package com.awakedw.feature.stats

import java.time.LocalDate

/**
 * 统计页周柱状图的纯几何换算（设计规格 §3.3 第 2 条）。
 *
 * 柱与目标线共用同一把刻度尺：以「周内最大柱量与目标量中的较大者」为刻度顶，
 * 映射到图表高 × [SCALE_FRACTION] 的刻度带内——最大柱至多顶到 0.86 倍图高，
 * 目标线无论高低都落在带内、与柱身保持真实比例。
 * 量值为 0 的柱归一到 0f 高度，由绘制层画成基线圆点（「这天还没喝」的温柔占位）。
 */
object StatsMath {
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

    /** 柱底标注：今天列写「今」，其余列写当月几号。 */
    fun columnLabels(
        dayKeys: List<String>,
        todayKey: String,
    ): List<String> =
        dayKeys.map { key ->
            if (key == todayKey) "今" else LocalDate.parse(key).dayOfMonth.toString()
        }

    /** 刻度顶取「最大柱、目标」中的较大者；全零周退化为目标量本身。 */
    private fun scaleMaxOf(
        values: List<Int>,
        goalMl: Int,
    ): Int = (values.maxOrNull() ?: 0).coerceAtLeast(goalMl).coerceAtLeast(1)

    private fun bandHeight(chartHeight: Float): Float = chartHeight * SCALE_FRACTION
}
