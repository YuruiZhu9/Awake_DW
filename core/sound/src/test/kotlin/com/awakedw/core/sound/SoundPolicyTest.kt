package com.awakedw.core.sound

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SoundPolicy 四分支覆盖（brief Step 1）。
 *
 * ringerMode 用 Int 字面量（控制器裁定，与 AudioManager 常量对齐）：
 * 2 = RINGER_MODE_NORMAL、0 = RINGER_MODE_SILENT、1 = RINGER_MODE_VIBRATE。
 */
class SoundPolicyTest {
    @Test
    fun `normal ringer with soundEnabled plays`() {
        // 2 == AudioManager.RINGER_MODE_NORMAL
        assertTrue(shouldPlay(ringerMode = 2, soundEnabled = true))
    }

    @Test
    fun `silent ringer does not play`() {
        // 0 == AudioManager.RINGER_MODE_SILENT
        assertFalse(shouldPlay(ringerMode = 0, soundEnabled = true))
    }

    @Test
    fun `vibrate ringer does not play`() {
        // 1 == AudioManager.RINGER_MODE_VIBRATE
        assertFalse(shouldPlay(ringerMode = 1, soundEnabled = true))
    }

    @Test
    fun `sound disabled does not play even in normal ringer`() {
        // 2 == AudioManager.RINGER_MODE_NORMAL
        assertFalse(shouldPlay(ringerMode = 2, soundEnabled = false))
    }
}
