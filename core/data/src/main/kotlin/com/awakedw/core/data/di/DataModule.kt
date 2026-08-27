package com.awakedw.core.data.di

import android.content.Context
import androidx.room.Room
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.SystemAppClock
import com.awakedw.core.data.db.AwakeDb
import com.awakedw.core.data.db.WaterRecordDao
import com.awakedw.core.data.repo.RoomWaterRepository
import com.awakedw.core.data.repo.WaterRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindWaterRepository(impl: RoomWaterRepository): WaterRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppClock(): AppClock = SystemAppClock()

        @Provides
        @Singleton
        fun provideAwakeDb(
            @ApplicationContext context: Context,
        ): AwakeDb = Room.databaseBuilder(context, AwakeDb::class.java, "awake_dw.db").build()

        @Provides
        fun provideWaterRecordDao(db: AwakeDb): WaterRecordDao = db.waterRecordDao()
    }
}
