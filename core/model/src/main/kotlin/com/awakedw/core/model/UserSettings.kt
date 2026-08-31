package com.awakedw.core.model

/**
 * 《中国居民膳食指南》推荐饮水量（用于健康贴士行展示）。
 * 中国营养学会建议成年居民每日饮水 1500～1700 毫升。
 */
const val RECOMMENDED_MIN_ML = 1500
const val RECOMMENDED_MAX_ML = 1700

/** 用户设置（持久化领域模型）。 */
data class UserSettings(
    // 每日目标
    val goalMl: Int = 1600,
    val cupMl: Int = 250,
    // 提醒时间窗（分钟数）：08:00–22:30
    val windowStartMin: Int = 480,
    val windowEndMin: Int = 1350,
    val intervalMin: Int = 90,
    val remindersEnabled: Boolean = true,
    val themeChoice: ThemeChoice = ThemeChoice.FOLLOW_TIME,
)
