package com.awakedw.core.notification

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.awakedw.core.model.TimeSlot
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * 场景 2/3：提醒到达发「温柔提醒」通知、动作按钮经 RecordingBroadcast 记一杯并把通知换成
 * 「记好啦 ♡」+ 2 秒自清。接收器经 EntryPointAccessors 手动注入（本仓 Hilt 2.52 + KSP 下
 * @AndroidEntryPoint 广播接收器不可用，回退路线在 Robolectric 中实证），测试作用域为
 * Unconfined，goAsync 协程体在 onReceive 内同步完成后才返回。
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
// 显式钉 SDK 35：库模块的 Robolectric 默认落在 minSdk=26，与生产目标（POST_NOTIFICATIONS 时代）不符。
@Config(application = HiltTestApplication::class, sdk = [35])
class ReminderReceiverRobolectricTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var scheduler: ReminderScheduler

    private lateinit var context: Context

    @Before
    fun setUp() {
        TestFakes.reset()
        context = RuntimeEnvironment.getApplication()
        // Robolectric 默认未授权 POST_NOTIFICATIONS，通知场景须显式授予（模拟 onboarding 主路径已请求）。
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        hiltRule.inject()
    }

    private fun notificationManager(): NotificationManager = context.getSystemService(NotificationManager::class.java)

    private fun postedNotification(): Notification = notificationManager().activeNotifications.single().notification

    private fun shadowAlarmManager() = shadowOf(context.getSystemService(android.app.AlarmManager::class.java))

    /** 场景 2：触发时发布通知——渠道「温柔提醒」IMPORTANCE_LOW，标题按 10 点=MORNING，动作指向 RecordingBroadcast。 */
    @Test
    fun `提醒到达_发布低重要性通知且动作指向RecordingBroadcast`() {
        ReminderReceiver().onReceive(context, Intent(context, ReminderReceiver::class.java))

        val channel = notificationManager().getNotificationChannel(NotifBuilder.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel!!.importance)
        assertEquals("温柔提醒", channel.name.toString())

        val notif = postedNotification()
        assertEquals("早安 ☀", notif.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("早一句", notif.extras.getString(Notification.EXTRA_TEXT))

        val action = notif.actions.single()
        assertEquals("喝啦 💧", action.title.toString())
        val savedIntent = shadowOf(action.actionIntent).savedIntent
        assertEquals(RecordingBroadcast::class.java.name, savedIntent.component!!.className)

        // 提醒链连续：发完本点即排下一点（10:00 + 90 分钟）。
        assertEquals(1, shadowAlarmManager().scheduledAlarms.size)
        assertEquals(atLocal(11, 30), shadowAlarmManager().scheduledAlarms.single().triggerAtMs)
        assertEquals(listOf(TimeSlot.MORNING), TestFakes.copies.picks)
    }

    /** 场景 3：点按「喝啦 💧」→ 同步落库一杯 cupMl，通知更新为「记好啦 ♡」且 setTimeoutAfter(2000)。 */
    @Test
    fun `点按喝啦_同步记一杯并把通知换成记好啦2秒自清`() {
        RecordingBroadcast().onReceive(context, Intent(context, RecordingBroadcast::class.java))

        val records = runBlocking { TestFakes.water.todayRecords() }
        assertEquals(1, records.size)
        assertEquals(250, records.single().amountMl)

        val notif = postedNotification()
        assertEquals("记好啦 ♡", notif.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(2000L, notif.timeoutAfter)
    }

    /** 规格补验：本次打卡使当日达标 → 达成日后取消剩余提醒（闹钟队列清空）。 */
    @Test
    fun `点按喝啦后当日达标_取消剩余提醒`() {
        // 先排上一点，模拟「剩余提醒在队」。
        scheduler.rescheduleFromNow(Reason.APP_START)
        assertEquals(1, shadowAlarmManager().scheduledAlarms.size)

        // 队列中的点已就位，本次打卡 250 使总量 1600 越线达标 → LOGGED 重算短路取消。
        runBlocking { TestFakes.water.addCup(1350) }
        RecordingBroadcast().onReceive(context, Intent(context, RecordingBroadcast::class.java))

        assertTrue(shadowAlarmManager().scheduledAlarms.isEmpty())
    }
}
