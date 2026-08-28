package com.awakedw.core.notification

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import javax.inject.Inject

/**
 * 场景 1/4/5 + 达标短路：NextReminderCalculator → ReminderScheduler → AlarmManager shadow
 * 的整链路断言（默认设置：窗口 08:00–22:30、间隔 90 分钟、当日 10:00 → 下一点 11:30）。
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
// 显式钉 SDK 35：库模块的 Robolectric 默认落在 minSdk=26，无法覆盖 ≥31 的精确闹钟降级分支。
@Config(application = HiltTestApplication::class, sdk = [35])
class NextReminderIntegrationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var scheduler: ReminderScheduler

    private lateinit var context: Context

    @Before
    fun setUp() {
        TestFakes.reset()
        context = RuntimeEnvironment.getApplication()
        hiltRule.inject()
    }

    private fun shadowAlarmManager(): ShadowAlarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))

    /** 默认设置在 10:00 的下一点：max(10:00, 08:00) + 90 分钟 = 11:30。 */
    private val expectedFireAt = atLocal(11, 30)

    @Test
    fun `开启提醒重排_队列恰有一条RTC_WAKEUP精确闹钟`() {
        scheduler.rescheduleFromNow(Reason.APP_START)

        val alarm = shadowAlarmManager().scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertEquals(expectedFireAt, alarm.triggerAtMs)
    }

    /** 场景 1：关闭 remindersEnabled 再 reschedule → 队列取消干净。 */
    @Test
    fun `关闭提醒后重排_闹钟队列清空`() {
        scheduler.rescheduleFromNow(Reason.APP_START)
        assertEquals(1, shadowAlarmManager().scheduledAlarms.size)

        runBlocking { TestFakes.prefs.setRemindersEnabled(false) }
        scheduler.rescheduleFromNow(Reason.SETTINGS_CHANGED)

        assertTrue(shadowAlarmManager().scheduledAlarms.isEmpty())
    }

    /** 场景 4（正路）：可精确闹钟时用 setExactAndAllowWhileIdle（窗口长度 0 / allowWhileIdle）。 */
    @Test
    fun `可精确闹钟时_走setExactAndAllowWhileIdle`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        scheduler.rescheduleFromNow(Reason.APP_START)

        val alarm = shadowAlarmManager().scheduledAlarms.single()
        assertEquals(expectedFireAt, alarm.triggerAtMs)
        assertEquals(0L, alarm.windowLengthMs)
        assertTrue(alarm.isAllowWhileIdle)
    }

    /** 场景 4（降级）：canScheduleExactAlarms=false 时降级 10 分钟窗口，而非精确闹钟。 */
    @Test
    fun `无法精确闹钟时_降级为10分钟窗口`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.rescheduleFromNow(Reason.APP_START)

        val alarm = shadowAlarmManager().scheduledAlarms.single()
        assertEquals(expectedFireAt, alarm.triggerAtMs)
        assertEquals(10 * 60_000L, alarm.windowLengthMs)
        assertFalse(alarm.isAllowWhileIdle)
    }

    /** 规格补验：今日已达标 → nextFire=null → 直接短路取消。 */
    @Test
    fun `当日已达标_重排即短路取消`() {
        scheduler.rescheduleFromNow(Reason.APP_START)
        assertEquals(1, shadowAlarmManager().scheduledAlarms.size)

        runBlocking { TestFakes.water.addCup(1600) } // total 1600 ≥ goal 1600
        scheduler.rescheduleFromNow(Reason.LOGGED)

        assertTrue(shadowAlarmManager().scheduledAlarms.isEmpty())
    }

    /** 场景 5：ACTION_BOOT_COMPLETED 广播后，闹钟队列重排一条。 */
    @Test
    fun `开机广播后_闹钟队列重排一条`() {
        val receiver = BootReceiver()

        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertEquals(expectedFireAt, shadowAlarmManager().scheduledAlarms.single().triggerAtMs)
    }
}
