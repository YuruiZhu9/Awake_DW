package com.awakedw.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.awakedw.core.common.AppClock
import com.awakedw.core.common.SystemAppClock
import com.awakedw.core.data.copy.DefaultCopyLibraryRepository
import com.awakedw.core.data.db.AwakeDb
import com.awakedw.core.data.db.WaterRecordDao
import com.awakedw.core.data.prefs.UserPreferencesRepositoryImpl
import com.awakedw.core.data.repo.RoomWaterRepository
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindWaterRepository(impl: RoomWaterRepository): WaterRepository

    @Binds
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    abstract fun bindCopyLibraryRepository(impl: DefaultCopyLibraryRepository): CopyLibraryRepository

    companion object {
        /** 全应用唯一的用户设置 DataStore（同一文件多实例会抛异常）。 */
        @Provides
        @Singleton
        fun provideUserPreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(produceFile = { File(context.filesDir, "awake_user.preferences_pb") })

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
