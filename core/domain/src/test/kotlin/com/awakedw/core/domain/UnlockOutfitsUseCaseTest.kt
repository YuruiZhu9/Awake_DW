package com.awakedw.core.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UnlockOutfitsUseCaseTest {
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var useCase: UnlockOutfitsUseCase

    @Before
    fun setUp() {
        prefs = FakeUserPreferencesRepository()
        useCase = UnlockOutfitsUseCase(prefs)
    }

    @Test
    fun `连胜7天_返回新解锁的dress01与dress02并落库`() =
        runBlocking {
            // 前情：开局件 dress_00 与 5 天档 museum_01 已在更早的同步中落库
            prefs.markOutfitsUnlocked(listOf("dress_00", "museum_01"))

            val new = useCase(7)

            assertEquals(listOf("dress_01", "dress_02"), new.map { it.id })
            assertEquals(setOf("dress_00", "museum_01", "dress_01", "dress_02"), prefs.unlockedOutfits.first())
        }

    @Test
    fun `重复调用幂等_再次同步返回空`() =
        runBlocking {
            prefs.markOutfitsUnlocked(listOf("dress_00", "museum_01"))
            useCase(7)

            val again = useCase(7)

            assertTrue(again.isEmpty())
        }

    @Test
    fun `连胜回落不回锁_已解锁藏品保留`() =
        runBlocking {
            prefs.markOutfitsUnlocked(listOf("dress_00", "museum_01"))
            useCase(7)

            val new = useCase(1)

            assertTrue(new.isEmpty())
            assertEquals(setOf("dress_00", "museum_01", "dress_01", "dress_02"), prefs.unlockedOutfits.first())
        }
}
