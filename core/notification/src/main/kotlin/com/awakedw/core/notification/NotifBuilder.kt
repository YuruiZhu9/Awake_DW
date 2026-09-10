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
 * 「喝水提醒」通知构建（§4.3）：低重要性渠道「喝水提醒」，时段标题映射，
 * 动作「记一杯」指向 [RecordingBroadcast]；打卡后以「已记一杯」+ 2 秒自清更新同 id 通知。
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

        /** 提醒通知：时段标题 + 文案库句子；点通知本体回应用（动作按钮才记水）。 */
        fun reminder(
            slot: TimeSlot,
            body: String,
        ): Notification {
            val openAppIntent =
                PendingIntent.getActivity(
                    context,
                    OPEN_APP_REQUEST_CODE,
                    // 库模块不依赖 :app——经包管理器取宿主启动意图（自家包恒有）。
                    context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val logAction =
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification_drop),
                    ACTION_LOG_WATER,
                    logWaterIntent(),
                ).build()
            return baseBuilder()
                .setContentTitle(titleOf(slot))
                .setContentText(body)
                .setContentIntent(openAppIntent)
                .addAction(logAction)
                .build()
        }

        /**
         * 打卡回执：同一通知原位换成一份**收据**——已记一杯，并交代当日进度。
         *
         * 回执**不得**复用 [titleOf]：那是时段问候，属于提醒通道的话。此前它被拿来当回执标题，
         * 于是使用者点完「记一杯」，通知变成「午安 ☀ / 已记一杯」——看上去像又来了一条提醒，
         * 把「这一下按对了」的确认冲掉。通知里同样要把两个通道分开（与首页环心确认同一原则）。
         *
         * 达标那次改说「今日份水灵达成 ✨」：使用者此刻盯着通知栏，看不到应用内的达标缎带，
         * 这里是唯一能告诉他的地方。文案与 `HomeScreen.CELEBRATION_TEXT` 刻意保持一致。
         */
        fun loggedAck(
            totalMl: Int,
            celebrated: Boolean,
        ): Notification =
            baseBuilder()
                .setContentTitle(LOGGED_TEXT)
                .setContentText(if (celebrated) LOGGED_GOAL_TEXT else loggedProgressText(totalMl))
                .setTimeoutAfter(ACK_TIMEOUT_MS)
                .build()

        private fun baseBuilder(): Notification.Builder =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_drop)
                .setAutoCancel(true)

        /** 「记一杯」动作的落点：同步记一杯并更新通知。 */
        private fun logWaterIntent(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                ACTION_REQUEST_CODE,
                Intent(context, RecordingBroadcast::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun hourNow(): Int = Instant.ofEpochMilli(clock.nowEpochMs()).atZone(clock.zone()).hour

        companion object {
            const val CHANNEL_ID = "gentle_reminder"
            const val CHANNEL_NAME = "喝水提醒"

            /** 全链路固定通知 id：提醒与打卡回执共用，实现「更新」而非叠加。 */
            const val NOTIFICATION_ID = 2001
            const val ACTION_LOG_WATER = "记一杯"

            /** 回执标题：陈述这一下已经生效，不带任何问候语。 */
            const val LOGGED_TEXT = "已记一杯"

            /** 本次打卡达成当日目标时的回执正文（与首页达标缎带同文案）。 */
            const val LOGGED_GOAL_TEXT = "今日份水灵达成 ✨"
            const val ACK_TIMEOUT_MS = 2_000L
            private const val ACTION_REQUEST_CODE = 2002
            private const val OPEN_APP_REQUEST_CODE = 2003

            /** 回执正文：交代记完之后的当日进度，单位写法与应用内一致（「250ml」无空格）。 */
            fun loggedProgressText(totalMl: Int): String = "今天共 ${totalMl}ml"

            /**
             * 时段 → 标题（§4.3）。DAY 覆盖 11:00–17:59，横跨中午与下午，
             * 取「午安」这类不指认具体时辰的说法，避免上午 11 点收到「下午好」。
             */
            fun titleOf(slot: TimeSlot): String =
                when (slot) {
                    TimeSlot.MORNING -> "早安 ☀"
                    TimeSlot.DAY -> "午安 ☀"
                    TimeSlot.EVENING -> "晚上好 🌙"
                }
        }
    }
