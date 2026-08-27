package com.awakedw.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DAY_KEY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Epoch 毫秒 → 该时刻在 [zone] 的本地日期键（yyyy-MM-dd），与 WaterRecord.dayKeyLocal 一致。 */
fun Long.toDayKey(zone: ZoneId): String = DAY_KEY_FORMAT.format(Instant.ofEpochMilli(this).atZone(zone))

/** 本地日期 → 日期键（yyyy-MM-dd），供本地日历回推区间使用。 */
fun LocalDate.toDayKey(): String = DAY_KEY_FORMAT.format(this)
