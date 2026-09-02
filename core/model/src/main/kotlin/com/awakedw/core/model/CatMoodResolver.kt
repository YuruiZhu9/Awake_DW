package com.awakedw.core.model

/** A brief response after logging, sleep at night, and idle otherwise. */
fun resolveCatMood(
    justCelebrated: Boolean,
    nowHour: Int,
): CatMood =
    when {
        justCelebrated -> CatMood.HAPPY
        nowHour >= 22 || nowHour < 6 -> CatMood.SLEEPY
        else -> CatMood.IDLE
    }
