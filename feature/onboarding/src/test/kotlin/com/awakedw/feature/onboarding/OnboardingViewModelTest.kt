package com.awakedw.feature.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 白名单引导 ViewModel（JVM + 假仓储）：
 * - complete 置位 onboarding_done 且触发完成接缝，mark-once：重复调用只落一次、回调只触发一次；
 * - 主按钮回执：跳转成功才完成；失败留在本页可重试、可跳过。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：假仓储 + 完成回调计数。 */
    private fun harness(): Harness {
        val prefs = FakePrefsRepository()
        val completions = mutableListOf<Int>()
        val viewModel = OnboardingViewModel(prefs) { completions += 1 }
        return Harness(prefs, completions, viewModel)
    }

    private class Harness(
        val prefs: FakePrefsRepository,
        val completions: MutableList<Int>,
        val viewModel: OnboardingViewModel,
    )

    @Test
    fun `complete置位onboardingDone并触发完成回调`() {
        val h = harness()

        h.viewModel.complete()

        assertEquals(1, h.prefs.markOnboardingCount)
        assertEquals(listOf(1), h.completions)
        assertTrue(h.viewModel.uiState.value.completed)
    }

    @Test
    fun `complete重复调用只标记一次且回调只触发一次`() {
        val h = harness()

        h.viewModel.complete()
        h.viewModel.complete()
        h.viewModel.complete()

        assertEquals(1, h.prefs.markOnboardingCount)
        assertEquals(listOf(1), h.completions)
    }

    @Test
    fun `主路径跳转成功回执完成引导`() {
        val h = harness()

        h.viewModel.onWhitelistJumpResult(success = true)

        assertEquals(1, h.prefs.markOnboardingCount)
        assertEquals(listOf(1), h.completions)
        assertTrue(h.viewModel.uiState.value.completed)
    }

    @Test
    fun `主路径跳转失败回执留在本页不置位`() {
        val h = harness()

        h.viewModel.onWhitelistJumpResult(success = false)

        assertEquals(0, h.prefs.markOnboardingCount)
        assertTrue(h.completions.isEmpty())
        assertFalse(h.viewModel.uiState.value.completed)
    }

    @Test
    fun `失败回执后重试成功仍能正常完成`() {
        val h = harness()

        h.viewModel.onWhitelistJumpResult(success = false)
        h.viewModel.onWhitelistJumpResult(success = true)

        assertEquals(1, h.prefs.markOnboardingCount)
        assertEquals(listOf(1), h.completions)
    }
}
