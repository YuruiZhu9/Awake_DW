package com.awakedw.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WaterRecordEntity::class], version = 1, exportSchema = false)
abstract class AwakeDb : RoomDatabase() {
    abstract fun waterRecordDao(): WaterRecordDao
}
