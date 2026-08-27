package com.awakedw.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.awakedw.core.model.WaterRecord

/** 表 water_record(id PK autogen, amount_ml, drank_at_epoch_ms INDEX, day_key_local INDEX)。 */
@Entity(
    tableName = "water_record",
    indices = [Index("drank_at_epoch_ms"), Index("day_key_local")],
)
data class WaterRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "amount_ml") val amountMl: Int,
    @ColumnInfo(name = "drank_at_epoch_ms") val drankAtEpochMs: Long,
    @ColumnInfo(name = "day_key_local") val dayKeyLocal: String,
)

fun WaterRecordEntity.toDomain(): WaterRecord =
    WaterRecord(id = id, amountMl = amountMl, drankAtEpochMs = drankAtEpochMs, dayKeyLocal = dayKeyLocal)
