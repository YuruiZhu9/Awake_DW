package com.awakedw.feature.settings

import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：假仓储 + （可选）提醒变更回调记录。 */
    private fun harness(
        settings: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
        onboardingDone: Boolean = true,
        library: CopyLibrary =
            CopyLibrary(
                morning = listOf("早安，喝水啦", "晨光和第一杯水"),
                day = listOf("午后补一杯"),
                evening = listOf("晚安前最后一杯"),
            ),
    ): Harness {
        val prefs = FakePrefsRepository(settings, onboardingDone)
        val copies = FakeCopyLibraryRepository(library)
        val water = FakeWaterRepository()
        val clock = FakeClock(REMINDER_TEST_TIME)
        val reminderCalls = mutableListOf<Boolean>()
        val testCalls = mutableListOf<Int>()
        val viewModel =
            SettingsViewModel(
                prefs = prefs,
                copies = copies,
                water = water,
                clock = clock,
                onRemindersChanged = { reminderCalls += it },
                onPostTestReminder = { testCalls += 1 },
            )
        return Harness(prefs, copies, water, clock, reminderCalls, testCalls, viewModel)
    }

    private class Harness(
        val prefs: FakePrefsRepository,
        val copies: FakeCopyLibraryRepository,
        val water: FakeWaterRepository,
        val clock: FakeClock,
        val reminderCalls: MutableList<Boolean>,
        val testCalls: MutableList<Int>,
        val viewModel: SettingsViewModel,
    )

    // region 状态灌入

    @Test
    fun `初始UiState灌入设置流文案库流与onboardingDone`() {
        val h = harness(onboardingDone = true)

        val state = h.viewModel.uiState.value
        assertEquals(1600, state.settings.goalMl)
        assertEquals("早安，喝水啦", state.library.morning.first())
        assertEquals(true, state.onboardingDone)
    }

    @Test
    fun `onboardingDone为false时UiState如实浮出false`() {
        val h = harness(onboardingDone = false)

        assertEquals(false, h.viewModel.uiState.value.onboardingDone)
    }

    @Test
    fun `仓储侧变更即时回灌UiState`() {
        val h = harness()

        h.viewModel.setGoalMl(2000)

        assertEquals(2000, h.viewModel.uiState.value.settings.goalMl)
    }

    // endregion

    // region 目标 / 一杯容量：合法直通、非法回落（绝不夹紧吸附）

    @Test
    fun `setGoalMl合法值直通仓储`() {
        val h = harness()

        h.viewModel.setGoalMl(2000)
        h.viewModel.setGoalMl(200)
        h.viewModel.setGoalMl(4000)

        assertEquals(listOf("goal=2000", "goal=200", "goal=4000"), h.prefs.calls)
        assertEquals(4000, h.viewModel.uiState.value.settings.goalMl)
    }

    @Test
    fun `setGoalMl非法值不落仓回落原值`() {
        val h = harness()

        listOf(150, 199, 225, 260, 4001, 4050).forEach { h.viewModel.setGoalMl(it) }

        assertEquals(emptyList<String>(), h.prefs.calls)
        assertEquals(1600, h.viewModel.uiState.value.settings.goalMl)
    }

    @Test
    fun `setCupMl合法值直通非法值回落`() {
        val h = harness()

        h.viewModel.setCupMl(300)
        h.viewModel.setCupMl(260) // 步进外
        h.viewModel.setCupMl(150) // 越下界

        assertEquals(listOf("cup=300"), h.prefs.calls)
        assertEquals(300, h.viewModel.uiState.value.settings.cupMl)
    }

    @Test
    fun `步进目标量乐观更新即生效_连点每次都基于最新值不丢步`() {
        val h = harness()

        // 快速连点：两次步进之间仓储尚未回灌，乐观更新保证第二次基于 1550 而非 1600。
        h.viewModel.stepGoalMl(-50)
        h.viewModel.stepGoalMl(-50)

        assertEquals(1500, h.viewModel.uiState.value.settings.goalMl)
        assertEquals(listOf("goal=1550", "goal=1500"), h.prefs.calls)
    }

    @Test
    fun `步进一杯容量同理且触界静默忽略`() {
        val h = harness()
        h.viewModel.stepCupMl(50)
        assertEquals(300, h.viewModel.uiState.value.settings.cupMl)
        assertEquals(listOf("cup=300"), h.prefs.calls)

        // 触下界：200 已是合法下限，再步进 50 直接忽略，不落库不吸附。
        val bottom = harness()
        bottom.viewModel.stepCupMl(-50) // 250 → 200
        bottom.viewModel.stepCupMl(-50) // 触界忽略
        assertEquals(200, bottom.viewModel.uiState.value.settings.cupMl)
        assertEquals(listOf("cup=200"), bottom.prefs.calls)
    }

    // endregion

    // region 提醒：间隔候选集 / 时段窗 / 总开关

    @Test
    fun `setIntervalMin候选集内直通集外回落原值90`() {
        val h = harness()

        h.viewModel.setIntervalMin(60)
        h.viewModel.setIntervalMin(50) // 不在候选集：回落 90，绝不吸附到 60
        h.viewModel.setIntervalMin(240)
        h.viewModel.setIntervalMin(75)

        assertEquals(listOf("interval=60", "interval=240"), h.prefs.calls)
        assertEquals(240, h.viewModel.uiState.value.settings.intervalMin)
    }

    @Test
    fun `setWindow合法窗直通仓储`() {
        val h = harness()

        h.viewModel.setWindow(540, 1290)

        assertEquals(listOf("window=540-1290"), h.prefs.calls)
        assertEquals(540, h.viewModel.uiState.value.settings.windowStartMin)
        assertEquals(1290, h.viewModel.uiState.value.settings.windowEndMin)
    }

    @Test
    fun `setWindow拒绝间隔不足倒挂与越界保持原窗`() {
        val h = harness()

        h.viewModel.setWindow(1000, 1005) // 间隔 5 分钟
        h.viewModel.setWindow(480, 510) // 恰好 30 分钟：要求严格大于
        h.viewModel.setWindow(1000, 480) // 倒挂
        h.viewModel.setWindow(299, 1000) // 起点越界
        h.viewModel.setWindow(300, 1395) // 终点越界
        h.viewModel.setWindow(455, 1000) // 非 15min 粒度

        assertEquals(emptyList<String>(), h.prefs.calls)
        assertEquals(480, h.viewModel.uiState.value.settings.windowStartMin)
        assertEquals(1350, h.viewModel.uiState.value.settings.windowEndMin)
    }

    @Test
    fun `setRemindersEnabled直通仓储并触发提醒变更回调`() {
        val h = harness()

        h.viewModel.setRemindersEnabled(false)
        h.viewModel.setRemindersEnabled(true)

        assertEquals(listOf("reminders=false", "reminders=true"), h.prefs.calls)
        assertEquals(listOf(false, true), h.reminderCalls)
        assertEquals(true, h.viewModel.uiState.value.settings.remindersEnabled)
    }

    // endregion

    // region 外观

    @Test
    fun `setThemeChoice直通仓储`() {
        val h = harness()

        h.viewModel.setThemeChoice(ThemeChoice.FIXED_STRAWBERRY)

        assertEquals(listOf("theme=FIXED_STRAWBERRY"), h.prefs.calls)
        assertEquals(ThemeChoice.FIXED_STRAWBERRY, h.viewModel.uiState.value.settings.themeChoice)
    }

    // endregion

    // region 心意文案库：编辑 / 新增 / 删除 / 恢复默认

    @Test
    fun `编辑文案替换组内条目并回灌UiState`() {
        val h = harness()

        h.viewModel.upsertCopy(TimeSlot.MORNING, 1, "改成只属于我们的一句话")

        assertEquals("改成只属于我们的一句话", h.copies.library.value.morning[1])
        assertEquals("改成只属于我们的一句话", h.viewModel.uiState.value.library.morning[1])
        assertEquals(2, h.viewModel.uiState.value.library.morning.size)
    }

    @Test
    fun `新增一句追加到组尾`() {
        val h = harness()

        h.viewModel.addCopy(TimeSlot.EVENING, "梦里也要记得喝水哦")

        val evening = h.viewModel.uiState.value.library.evening
        assertEquals(2, evening.size)
        assertEquals("梦里也要记得喝水哦", evening.last())
    }

    @Test
    fun `删除组内条目即时回灌`() {
        val h = harness()

        h.viewModel.deleteCopy(TimeSlot.DAY, 0)

        assertEquals(emptyList<String>(), h.viewModel.uiState.value.library.day)
    }

    @Test
    fun `恢复默认文案整库重置`() {
        val h = harness()

        h.viewModel.resetCopyLibrary()

        assertEquals(1, h.copies.resetCount)
        assertEquals("默认早安", h.viewModel.uiState.value.library.morning.single())
    }

    @Test
    fun `编辑文案空文本与超40字一律不落库`() {
        val h = harness()

        h.viewModel.upsertCopy(TimeSlot.MORNING, 0, "   ")
        h.viewModel.upsertCopy(TimeSlot.MORNING, 0, "好".repeat(41))
        h.viewModel.addCopy(TimeSlot.MORNING, "")

        assertEquals("早安，喝水啦", h.viewModel.uiState.value.library.morning[0])
        assertEquals(2, h.viewModel.uiState.value.library.morning.size)
        assertTrue(h.prefs.calls.isEmpty())
    }

    @Test
    fun `编辑文案保存前去除首尾空白`() {
        val h = harness()

        h.viewModel.upsertCopy(TimeSlot.DAY, 0, "  午后记得喝水  ")

        assertEquals("午后记得喝水", h.viewModel.uiState.value.library.day.single())
    }

    // endregion

    // region 提醒透明化（§11.3/11.4）：状态行计算与试一发接缝

    @Test
    fun `窗口内有排程时状态行显示下一次时刻`() {
        val h = harness()

        // 默认设置：12:00 在 08:00–22:30 窗内、间隔 90 分钟 → 下一次 13:30。
        assertEquals("下一次提醒 · 今天 13:30", h.viewModel.uiState.value.reminderStatusLabel)
        assertEquals(true, h.viewModel.uiState.value.reminderArmed)
    }

    @Test
    fun `关闭提醒后状态行为已关闭且不设排程`() {
        val h = harness()

        h.viewModel.setRemindersEnabled(false)

        assertEquals("提醒已关闭 · 暂时不提醒", h.viewModel.uiState.value.reminderStatusLabel)
        assertEquals(false, h.viewModel.uiState.value.reminderArmed)
    }

    @Test
    fun `今日已达标时状态行提示明天继续`() {
        val h = harness()
        h.water.stats = com.awakedw.core.model.DailyStats(totalMl = 1600, cupCount = 8, avgIntervalMin = null)

        // 设置流再发一次（任意合法变更）触发状态重算。
        h.viewModel.setIntervalMin(60)

        assertEquals("今天的目标已完成 · 明天继续", h.viewModel.uiState.value.reminderStatusLabel)
        assertEquals(false, h.viewModel.uiState.value.reminderArmed)
    }

    @Test
    fun `试一试调用接缝并短暂回显`() =
        kotlinx.coroutines.test.runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val h = harness()
                h.viewModel.testReminder()

                assertEquals(1, h.testCalls.size)
                assertEquals(true, h.viewModel.uiState.value.testReminderSent)

                testScheduler.advanceTimeBy(TEST_SENT_HOLD_MS + 1)
                assertEquals(false, h.viewModel.uiState.value.testReminderSent)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private companion object {
        /** 提醒状态行测试锚点：2026-08-29 12:00（Asia/Shanghai，默认窗口内）。 */
        val REMINDER_TEST_TIME: Long =
            java.time.ZonedDateTime.of(2026, 8, 29, 12, 0, 0, 0, java.time.ZoneId.of("Asia/Shanghai"))
                .toInstant().toEpochMilli()
    }
}
