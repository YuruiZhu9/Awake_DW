package com.awakedw.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * 唯一 Activity 宿主：系统闪屏驻留至组合就绪，随后交由 Compose 内
 * [SplashMorph] 续场接管（水滴落点 → 涟漪 → 形序入首页）。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** 系统闪屏放行开关：Compose 首帧挂载（SideEffect）即置真。 */
    private var entryReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !entryReady }
        super.onCreate(savedInstanceState)
        setContent {
            AwakeApp(onEntryReady = { entryReady = true })
        }
    }
}
