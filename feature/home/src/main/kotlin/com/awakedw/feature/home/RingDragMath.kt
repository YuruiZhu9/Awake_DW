package com.awakedw.feature.home

import com.awakedw.feature.home.components.roundTo10
import kotlin.math.roundToInt

/**
 * 环即把手的纯换算（2.2.0）：沿环扫过的角度 → 记录毫升数，整圈恰好一杯容量。
 * 换算归 feature 层——designsystem 只收发几何角度，不感知毫升与业务（见 v220 计划 §3）。
 */
internal object RingDragMath {
    /** 提交阈值：松手时扫过角度小于此值视为误触，静默取消，不成笔也不回显。 */
    const val COMMIT_THRESHOLD_DEGREES = 15f

    /**
     * 扫过角 [sweptDegrees]（顺时针为正，度）换算为毫升，取 10 的倍数与快捷量同一刻度，整圈恰好一杯。
     * 阈值内或倒拨（负角）返回 null——不记录；就近取整后不足 10ml 同样返回 null，杜绝 0ml 记录。
     */
    fun sweepToMl(
        sweptDegrees: Float,
        cupMl: Int,
    ): Int? {
        if (sweptDegrees < COMMIT_THRESHOLD_DEGREES) return null
        val ml = roundTo10((cupMl * sweptDegrees / 360f).roundToInt())
        return if (ml <= 0) null else ml
    }
}
