package com.awakedw.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 胆大王状态纯函数：心情判定与配饰解锁曲线。 */
class CatTest {
    @Test
    fun `resolveCatMood - 刚庆祝优先返回HAPPY`() {
        assertEquals(CatMood.HAPPY, resolveCatMood(justCelebrated = true, nowHour = 14))
    }

    @Test
    fun `resolveCatMood - 深夜窗22至6点半开区间返回SLEEPY`() {
        assertEquals(CatMood.SLEEPY, resolveCatMood(justCelebrated = false, nowHour = 23))
        assertEquals(CatMood.SLEEPY, resolveCatMood(justCelebrated = false, nowHour = 22))
        assertEquals(CatMood.SLEEPY, resolveCatMood(justCelebrated = false, nowHour = 0))
        assertEquals(CatMood.SLEEPY, resolveCatMood(justCelebrated = false, nowHour = 5))
    }

    @Test
    fun `resolveCatMood - 白天未庆祝返回IDLE`() {
        assertEquals(CatMood.IDLE, resolveCatMood(justCelebrated = false, nowHour = 14))
        assertEquals(CatMood.IDLE, resolveCatMood(justCelebrated = false, nowHour = 6))
        assertEquals(CatMood.IDLE, resolveCatMood(justCelebrated = false, nowHour = 7))
        assertEquals(CatMood.IDLE, resolveCatMood(justCelebrated = false, nowHour = 21))
    }

    @Test
    fun `resolveCatMood - 庆祝在深夜仍优先HAPPY`() {
        assertEquals(CatMood.HAPPY, resolveCatMood(justCelebrated = true, nowHour = 23))
    }

    @Test
    fun `unlockedCatAccessories - 30天解锁全部三件且按解锁日升序`() {
        val unlocked = unlockedCatAccessories(30)
        assertEquals(listOf(CatAccessory.BOW, CatAccessory.PEARL, CatAccessory.OUTFIT), unlocked)
    }

    @Test
    fun `unlockedCatAccessories - 2天一件未解锁`() {
        assertTrue(unlockedCatAccessories(2).isEmpty())
    }

    @Test
    fun `unlockedCatAccessories - 边界日恰好解锁对应配饰`() {
        assertEquals(listOf(CatAccessory.BOW), unlockedCatAccessories(3))
        assertEquals(listOf(CatAccessory.BOW, CatAccessory.PEARL), unlockedCatAccessories(14))
    }
}
