package com.awakedw.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 提醒状态行测试锚点（本地锚：2026-08-27 10:00 Asia/Shanghai，落在清醒窗内）。 */
private val SOUND_TEST_TIME: Long =
    LocalDateTime.of(2026, 8, 27, 10, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

/**
 * 声音开关（任务 12）：
 * - init 收集 `prefs.soundEnabled` 灌入 UiState（重进页面如实回显持久值）；
 * - `setSoundEnabled` **乐观更新**——先翻本地 state 再落库（语义同目标量步进器），
 *   prefs 假件按序收到 `setSoundEnabled` 轨迹。
 *
 * 设置页开关只写 prefs、不直接调播放器（播确认音无必要）：静音裁决在 SoundPoolPlayer 内部。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSoundTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：假仓储 + 默认空副作用接缝（与 SettingsViewModelTest 同款）。 */
    private fun harness(initialSoundEnabled: Boolean = true): Harness {
        val prefs = FakePrefsRepository(initialSoundEnabled = initialSoundEnabled)
        val copies = FakeCopyLibraryRepository()
        val water = FakeWaterRepository()
        val clock = FakeClock(SOUND_TEST_TIME)
        val viewModel =
            SettingsViewModel(
                prefs = prefs,
                copies = copies,
                water = water,
                clock = clock,
            )
        return Harness(prefs, viewModel)
    }

    private class Harness(
        val prefs: FakePrefsRepository,
        val viewModel: SettingsViewModel,
    )

    @Test
    fun `初始UiState灌入prefs的音效开关`() {
        val off = harness(initialSoundEnabled = false)
        assertEquals(false, off.viewModel.uiState.value.soundEnabled)

        val on = harness(initialSoundEnabled = true)
        assertEquals(true, on.viewModel.uiState.value.soundEnabled)
    }

    @Test
    fun `切换开关乐观翻转state并按序落库`() {
        val h = harness()

        h.viewModel.setSoundEnabled(false)
        // 乐观语义：本地 state 即刻翻转，不等 DataStore 写回。
        assertEquals(false, h.viewModel.uiState.value.soundEnabled)
        assertEquals(listOf("sound=false"), h.prefs.calls)

        h.viewModel.setSoundEnabled(true)
        assertEquals(true, h.viewModel.uiState.value.soundEnabled)
        assertEquals(listOf("sound=false", "sound=true"), h.prefs.calls)
    }
}
