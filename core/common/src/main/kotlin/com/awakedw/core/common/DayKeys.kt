package com.awakedw.core.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Epoch 毫秒 → 该时刻在 [zone] 的本地日期键（yyyy-MM-dd），与 WaterRecord.dayKeyLocal 一致。 */
fun Long.toDayKey(zone: ZoneId): String = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(Instant.ofEpochMilli(this).atZone(zone))
