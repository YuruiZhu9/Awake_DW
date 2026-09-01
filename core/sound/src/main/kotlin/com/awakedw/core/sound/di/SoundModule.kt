package com.awakedw.core.sound.di

import com.awakedw.core.sound.AwakeSoundPlayer
import com.awakedw.core.sound.SoundPoolPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 声音门面绑定：调用方一律面向 [AwakeSoundPlayer]，SoundPool 细节不出 :core:sound。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SoundModule {
    @Binds
    abstract fun bindAwakeSoundPlayer(impl: SoundPoolPlayer): AwakeSoundPlayer
}
