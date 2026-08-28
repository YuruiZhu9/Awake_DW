package com.awakedw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 开屏续场时序状态机（规格 §2.3）：
 * - 点击任意处即完成跳转（canSkip → DONE）；
 * - 自然播放 ~1200ms 内走完三段时序自动放行；
 * - 兜底契约：无论掉帧多严重，全程超 1400ms 必须强制放行。
 *
 * 纯 JVM 虚拟时钟推帧即可测，不依赖 Robolectric。
 */
class SplashSequencingTest {
    @Test
    fun `点击任意处立即完成跳转`() {
        val sequencer = SplashSequencer()
        sequencer.tick(FRAME_MS)

        assertTrue("播放期间任意时刻均可跳过", sequencer.canSkip)

        sequencer.skip()

        assertEquals(SplashPhase.DONE, sequencer.phase)
        assertTrue(sequencer.skipped)
        assertFalse("放行后无需再跳", sequencer.canSkip)
    }

    @Test
    fun `自然放行后再点击不误标跳过`() {
        val sequencer = SplashSequencer()
        sequencer.tick(2_000)

        assertEquals(SplashPhase.DONE, sequencer.phase)

        sequencer.skip()

        assertFalse(sequencer.skipped)
    }

    @Test
    fun `自然播放1200ms内走完三段时序自动放行`() {
        val sequencer = SplashSequencer()
        var elapsedMs = 0L
        while (sequencer.phase != SplashPhase.DONE && elapsedMs <= MAX_OBSERVE_MS) {
            sequencer.tick(FRAME_MS)
            elapsedMs += FRAME_MS
        }

        assertEquals(SplashPhase.DONE, sequencer.phase)
        assertFalse(sequencer.skipped)
        assertTrue("应在 1200ms 时序完成后、1400ms 上限内放行，实际 ${elapsedMs}ms", elapsedMs <= MAX_DWELL_MS)
    }

    @Test
    fun `时序异常拉长时超过1400ms强制放行兜底`() {
        // 三段全被拉长到 3s：只有兜底上限能救——模拟主线程掉帧饥饿。
        val sequencer = SplashSequencer(dropletMs = 3_000, rippleMs = 3_000, morphMs = 3_000)
        var elapsedMs = 0L
        while (sequencer.phase != SplashPhase.DONE && elapsedMs <= MAX_OBSERVE_MS) {
            sequencer.tick(FRAME_MS)
            elapsedMs += FRAME_MS
        }

        assertEquals(SplashPhase.DONE, sequencer.phase)
        // 帧量化：兜底在「首个 ≥1400ms 的帧」放行，驻留上限 = 1400ms + 单帧。
        assertTrue("兜底上限应为 1400ms+单帧，实际 ${elapsedMs}ms", elapsedMs < MAX_DWELL_MS + FRAME_MS)
    }

    @Test
    fun `相位按水滴_涟漪_形序时间轴推进`() {
        val sequencer = SplashSequencer()

        sequencer.tick(SPLASH_DROPLET_MS - 1)
        assertEquals(SplashPhase.DROPLET, sequencer.phase)

        sequencer.tick(1) // 450ms：水滴落地，涟漪起
        assertEquals(SplashPhase.RIPPLE, sequencer.phase)

        sequencer.tick(500) // 950ms：两圈涟漪收束，形序起
        assertEquals(SplashPhase.MORPH, sequencer.phase)

        sequencer.tick(SPLASH_MORPH_MS - 1) // 1199ms：形序未完
        assertEquals(SplashPhase.MORPH, sequencer.phase)

        sequencer.tick(1) // 1200ms：放行
        assertEquals(SplashPhase.DONE, sequencer.phase)
    }

    @Test
    fun `水滴进度带过冲回弹且各段进度有界`() {
        val sequencer = SplashSequencer()
        sequencer.tick(SPLASH_DROPLET_MS / 2)
        val mid = sequencer.snapshot()
        assertTrue("水滴下落中应有过冲峰值(>0.5)", mid.dropletProgress > 0.5f)
        assertTrue(mid.dropletProgress <= DROPLET_OVERSHOOT_MAX)

        sequencer.tick(2_000)
        val settled = sequencer.snapshot()
        assertEquals(1f, settled.dropletProgress)
        assertEquals(1f, settled.ripple1Progress)
        assertEquals(1f, settled.ripple2Progress)
        assertEquals(1f, settled.morphProgress)
    }

    @Test
    fun `时间倒退的帧不推进状态`() {
        val sequencer = SplashSequencer()
        sequencer.tick(100)
        sequencer.tick(-500)

        assertEquals(100L, sequencer.snapshot().elapsedMs)
        assertEquals(SplashPhase.DROPLET, sequencer.phase)
    }

    @Test
    fun `重建后按已播毫秒跳相_不重放开屏`() {
        // 旋转前的播放进度：水滴已收束，处于涟漪段中段。
        val beforeRotation = SplashSequencer()
        beforeRotation.tick(600)

        // 旋转重建：新状态机一次性恢复已播毫秒。
        val restored = SplashSequencer()
        restored.restore(beforeRotation.elapsedMs)

        assertEquals(SplashPhase.RIPPLE, restored.phase)
        assertEquals(beforeRotation.elapsedMs, restored.elapsedMs)

        // 恢复后剩余时序继续走完并自然放行：观察窗 < 全程 1200ms，证明没有从水滴重放。
        var elapsedMs = 0L
        while (restored.phase != SplashPhase.DONE && elapsedMs <= MAX_OBSERVE_MS) {
            restored.tick(FRAME_MS)
            elapsedMs += FRAME_MS
        }
        assertEquals(SplashPhase.DONE, restored.phase)
        assertFalse(restored.skipped)
        assertTrue("恢复后续播应小于全程重放（实际 ${elapsedMs}ms）", elapsedMs < SPLASH_TOTAL_MS)
    }

    @Test
    fun `恢复至已过形序收束的时刻_立即放行`() {
        val restored = SplashSequencer()
        restored.restore(2_000)

        assertEquals(SplashPhase.DONE, restored.phase)
        assertFalse(restored.skipped)
    }

    @Test
    fun `restore负值与已放行状态被忽略`() {
        val fresh = SplashSequencer()
        fresh.restore(-5)
        assertEquals(0L, fresh.elapsedMs)
        assertEquals(SplashPhase.DROPLET, fresh.phase)

        val done = SplashSequencer()
        done.tick(2_000)
        done.restore(100)
        assertEquals(2_000L, done.elapsedMs)
        assertEquals(SplashPhase.DONE, done.phase)
    }

    private companion object {
        const val FRAME_MS = 16L
        const val SPLASH_DROPLET_MS = 450L
        const val SPLASH_MORPH_MS = 250L
        const val SPLASH_TOTAL_MS = 1_200L
        const val MAX_DWELL_MS = 1_400L
        const val MAX_OBSERVE_MS = 5_000L
        const val DROPLET_OVERSHOOT_MAX = 1.2f
    }
}
