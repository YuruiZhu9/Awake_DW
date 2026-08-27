package com.awakedw.core.common

import java.time.ZoneId

/** 时钟抽象：业务代码只依赖它获取当前时间与时区，测试中可替换为固定时钟。 */
interface AppClock {
    fun nowEpochMs(): Long

    fun zone(): ZoneId
}
