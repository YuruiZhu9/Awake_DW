package com.awakedw.core.sound

import android.media.AudioManager

/**
 * 声音播放策略（纯函数）：系统静音遵从优先于应用内开关。
 *
 * 仅在「铃声模式为 NORMAL 且用户开启音效」时才允许出声；
 * SILENT / VIBRATE 模式一律静音，soundEnabled=false 亦不发声。
 * ringerMode 取值来自 AudioManager.getRingerMode()（RINGER_MODE_NORMAL=2 / SILENT=0 / VIBRATE=1）。
 */
fun shouldPlay(
    ringerMode: Int,
    soundEnabled: Boolean,
): Boolean = ringerMode == AudioManager.RINGER_MODE_NORMAL && soundEnabled
