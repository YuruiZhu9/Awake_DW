package com.awakedw.feature.settings

/**
 * 设置项校验规则（纯逻辑，JVM 可测）。
 *
 * 全部 setter 共用一条铁律：**非法输入回落原值，绝不夹紧吸附**——
 * 例如目标量 225 不被吸到 250、间隔 50 不被吸到 60，原值原样保留。
 */
object SettingsValidation {
    /** 目标量 / 一杯容量的合法区间与步进：∈[200..4000]，步进 50。 */
    const val ML_MIN = 200

    const val ML_MAX = 4000

    const val ML_STEP = 50

    /** 提醒间隔候选档（分钟）：仅接受这七档，集外回落原值。 */
    val INTERVAL_CHOICES: List<Int> = listOf(30, 60, 90, 120, 150, 180, 240)

    /** 清醒时段滑杆的起止边界（分钟）：05:00 – 23:00。 */
    const val WINDOW_MIN = 300

    const val WINDOW_MAX = 1380

    /** 清醒时段滑杆粒度：15 分钟。 */
    const val WINDOW_GRANULARITY_MIN = 15

    /** 起止最小间隔：start < end − 30（严格大于，间隔恰为 30 也拒绝）。 */
    const val WINDOW_GAP_MIN = 30

    /** [ml] 是否为合法的毫升设置（目标量 / 一杯容量共用）：区间内且整除步进。 */
    fun isValidMl(ml: Int): Boolean = ml in ML_MIN..ML_MAX && ml % ML_STEP == 0

    /** [intervalMin] 是否为合法提醒间隔：必须落在候选档集合内。 */
    fun isValidInterval(intervalMin: Int): Boolean = intervalMin in INTERVAL_CHOICES

    /**
     * [startMin]/[endMin] 是否为合法清醒时段：
     * 起止都 ∈[300..1380]、都落在 15 分钟粒度上、且严格 start < end − 30。
     */
    fun isValidWindow(
        startMin: Int,
        endMin: Int,
    ): Boolean =
        startMin in WINDOW_MIN..WINDOW_MAX &&
            endMin in WINDOW_MIN..WINDOW_MAX &&
            startMin % WINDOW_GRANULARITY_MIN == 0 &&
            endMin % WINDOW_GRANULARITY_MIN == 0 &&
            startMin < endMin - WINDOW_GAP_MIN
}
