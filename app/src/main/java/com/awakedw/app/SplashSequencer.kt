package com.awakedw.app

/** 开屏续场阶段：水滴 → 涟漪 → 形序入首页 → 放行。 */
enum class SplashPhase {
    DROPLET,
    RIPPLE,
    MORPH,
    DONE,
}

/** 某一帧的时序快照：各段进度已按缓动整形，渲染层零业务。 */
data class SplashFrame(
    val phase: SplashPhase,
    val elapsedMs: Long,
    /** 水滴落点进度：带 spring 观感的过冲回弹，峰值约 1.13，终值恒 1。 */
    val dropletProgress: Float,
    /** 第一圈涟漪（先起先收）。 */
    val ripple1Progress: Float,
    /** 第二圈涟漪（相位差 120ms）。 */
    val ripple2Progress: Float,
    /** 涟漪外圈放大为进度环初始态 + Crossfade 入首页的进度。 */
    val morphProgress: Float,
    val skipped: Boolean,
)

/**
 * 开屏续场时序状态机（纯 JVM）：Compose 帧驱动与单测共用同一套契约。
 *
 * 时间轴（总 ~1200ms，规格 §2.3）：
 * - DROPLET 0–450ms：水滴自上而下 spring 落点（过冲回弹）；
 * - RIPPLE 450–950ms：两圈涟漪先后扩散（各 380ms，相位差 120ms）；
 * - MORPH 950–1200ms：外圈涟漪放大为进度环初始态并 Crossfade 250ms 入首页。
 *
 * 兜底契约：全程超 [FORCE_DONE_MS] 即强制放行——掉帧/异常场景下首页必须可达。
 */
class SplashSequencer(
    dropletMs: Long = DROPLET_MS,
    rippleMs: Long = RIPPLE_MS,
    morphMs: Long = MORPH_MS,
    private val forceDoneMs: Long = FORCE_DONE_MS,
) {
    /** 三段时序边界：水滴收束 / 涟漪收束 / 形序收束（放行首页）。 */
    private val dropletEndMs = dropletMs

    /** 第一圈涟漪先起先收：相位差 120ms 由第二圈的起点承担。 */
    private val ripple1EndMs = dropletEndMs + (rippleMs - RIPPLE_PHASE_OFFSET_MS)
    private val rippleEndMs = dropletEndMs + rippleMs
    private val morphEndMs = rippleEndMs + morphMs

    var phase: SplashPhase = SplashPhase.DROPLET
        private set

    var elapsedMs: Long = 0L
        private set

    /** 是否由用户点击跳过（区别于自然放行）。 */
    var skipped: Boolean = false
        private set

    /** 播放期间点击任意处随时可跳；已放行则无需再跳。 */
    val canSkip: Boolean
        get() = phase != SplashPhase.DONE

    /**
     * 重建恢复（旋转等配置变更后不重放开屏）：
     * 新状态机一次性跳相到 [restoredElapsedMs]（语义等同把该值当作已播时长），
     * 超过形序收束即立即放行；非正数与已放行后的恢复一律忽略。
     */
    fun restore(restoredElapsedMs: Long) {
        if (restoredElapsedMs <= 0L || phase == SplashPhase.DONE) return
        tick(restoredElapsedMs)
    }

    /** 推进一帧（虚拟时钟推帧即可测）；时间倒退与已放行后的帧一律忽略。 */
    fun tick(deltaMs: Long) {
        if (phase == SplashPhase.DONE || deltaMs <= 0L) return
        elapsedMs += deltaMs
        phase =
            when {
                elapsedMs >= forceDoneMs || elapsedMs >= morphEndMs -> SplashPhase.DONE
                elapsedMs >= rippleEndMs -> SplashPhase.MORPH
                elapsedMs >= dropletEndMs -> SplashPhase.RIPPLE
                else -> SplashPhase.DROPLET
            }
    }

    /** 用户点击：状态机立即放行进入首页。 */
    fun skip() {
        if (!canSkip) return
        skipped = true
        phase = SplashPhase.DONE
    }

    fun snapshot(): SplashFrame =
        SplashFrame(
            phase = phase,
            elapsedMs = elapsedMs,
            dropletProgress = springFall(elapsedMs),
            ripple1Progress = segmentProgress(elapsedMs, dropletEndMs, ripple1EndMs),
            ripple2Progress = segmentProgress(elapsedMs, dropletEndMs + RIPPLE_PHASE_OFFSET_MS, rippleEndMs),
            morphProgress = segmentProgress(elapsedMs, rippleEndMs, morphEndMs),
            skipped = skipped,
        )

    /** Overshoot(tension=2) 等效 spring 落点：越过落点约 13% 再回弹。 */
    private fun springFall(elapsedMs: Long): Float {
        val t = segmentProgress(elapsedMs, 0L, dropletEndMs)
        if (t >= 1f) return 1f
        if (t <= 0f) return 0f
        val u = t - 1f
        return 1f + u * u * (3f * u + 2f)
    }

    private fun segmentProgress(
        elapsedMs: Long,
        startMs: Long,
        endMs: Long,
    ): Float {
        if (endMs <= startMs) return 1f
        if (elapsedMs <= startMs) return 0f
        if (elapsedMs >= endMs) return 1f
        return (elapsedMs - startMs).toFloat() / (endMs - startMs).toFloat()
    }

    private companion object {
        const val DROPLET_MS = 450L
        const val RIPPLE_MS = 500L // 两圈各 380ms + 相位差 120ms
        const val MORPH_MS = 250L
        const val FORCE_DONE_MS = 1_400L
        const val RIPPLE_PHASE_OFFSET_MS = 120L
    }
}
