package com.awakedw.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** 应用宿主：@HiltAndroidApp 生成 SingletonComponent，为全图闭环的装配入口。 */
@HiltAndroidApp
class AwakeApplication : Application()
