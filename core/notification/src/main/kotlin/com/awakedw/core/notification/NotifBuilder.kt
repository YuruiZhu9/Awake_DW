package com.awakedw.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/**
 * 「温柔提醒」通知构建（§4.3）：低重要性渠道「温柔提醒」，时段标题映射，
 * 动作「喝啦 💧」指向 [RecordingBroadcast]；打卡后以「记好啦 ♡」+ 2 秒自清更新同 id 通知。
 */
class NotifBuilder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: AppClock,
    ) {
        /** 当前时刻所属时段（标题映射与文案抽取共用）。 */
        fun currentSlot(): TimeSlot = TimeSlots.slotOfHour(hourNow())

        /** 幂等建渠道（minSdk 26，无需版本分支）并按权限发布；未授权时静默跳过。 */
        fun post(notification: Notification) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW))
            if (manager.areNotificationsEnabled()) {
                manager.notify(NOTIFICATION_ID, notification)
            }
        }

        /** 提醒通知：时段标题 + 文案库句子 + 「喝啦 💧」动作（指向 [RecordingBroadcast]）。 */
        fun reminder(
            slot: TimeSlot,
            body: String,
        ): Notification {
            val contentIntent =
                PendingIntent.getBroadcast(
                    context,
                    ACTION_REQUEST_CODE,
                    Intent(context, RecordingBroadcast::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val logAction =
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification_drop),
                    ACTION_LOG_WATER,
                    contentIntent,
                ).build()
            return baseBuilder()
                .setContentTitle(titleOf(slot))
                .setContentText(body)
                .setContentIntent(contentIntent)
                .addAction(logAction)
                .build()
        }

        /** 打卡回执通知：同 id 更新为「记好啦 ♡」，2 秒后自动撤销。 */
        fun loggedAck(): Notification =
            baseBuilder()
                .setContentTitle(titleOf(currentSlot()))
                .setContentText(LOGGED_TEXT)
                .setTimeoutAfter(ACK_TIMEOUT_MS)
                .build()

        private fun baseBuilder(): Notification.Builder =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_drop)
                .setAutoCancel(true)

        private fun hourNow(): Int = Instant.ofEpochMilli(clock.nowEpochMs()).atZone(clock.zone()).hour

        companion object {
            const val CHANNEL_ID = "gentle_reminder"
            const val CHANNEL_NAME = "温柔提醒"

            /** 全链路固定通知 id：提醒与打卡回执共用，实现「更新」而非叠加。 */
            const val NOTIFICATION_ID = 2001
            const val ACTION_LOG_WATER = "喝啦 💧"
            const val LOGGED_TEXT = "记好啦 ♡"
            const val ACK_TIMEOUT_MS = 2_000L
            private const val ACTION_REQUEST_CODE = 2002

            /** 时段 → 标题（§4.3）。 */
            fun titleOf(slot: TimeSlot): String =
                when (slot) {
                    TimeSlot.MORNING -> "早安 ☀"
                    TimeSlot.DAY -> "午后啦 ☀"
                    TimeSlot.EVENING -> "晚上好 🌙"
                }
        }
    }
