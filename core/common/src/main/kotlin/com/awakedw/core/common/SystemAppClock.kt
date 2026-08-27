package com.awakedw.core.common

import java.time.ZoneId

/**
 * 系统真实时钟。DI 绑定（Hilt）随后续 :core:data 引入，
 * 此处保持纯 Kotlin 无注解。
 */
class SystemAppClock : AppClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
