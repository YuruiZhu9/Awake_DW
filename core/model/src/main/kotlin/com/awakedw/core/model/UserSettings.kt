package com.awakedw.core.model

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
