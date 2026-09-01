package com.awakedw.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** 掉落音微变调下界（-6%，听感是「轻轻换了一颗水滴」，重复落杯不呆板）。 */
internal const val DROP_RATE_MIN = 0.94f

/** 掉落音微变调上界（+6%）。 */
internal const val DROP_RATE_MAX = 1.06f

/**
 * 呼噜声固定速率：略降速降调（-5%），音色更低沉温暖，贴合「满足的猫」的体感；
 * 不做随机偏移——呼噜是长音，每次一致的音色比变化更安神。
 */
internal const val PURR_RATE = 0.95f

/**
 * 播放速率裁决（纯函数，便于无 Robolectric 断言）：
 * - DROP_*：随机微变调 ∈ [0.94, 1.06]，同一事件连响也不呆板；
 * - GOAL_MELODY：固定 1.0f——旋律是庆祝主唱，不走音；
 * - PURR：固定 0.95f——略低沉更温暖（取舍见 [PURR_RATE]）。
 */
internal fun playbackRateFor(
    event: SoundEvent,
    random: Random,
): Float =
    when (event) {
        SoundEvent.GOAL_MELODY -> 1.0f
        SoundEvent.PURR -> PURR_RATE
        SoundEvent.DROP_A, SoundEvent.DROP_B, SoundEvent.DROP_C ->
            DROP_RATE_MIN + random.nextFloat() * (DROP_RATE_MAX - DROP_RATE_MIN)
    }

/**
 * [AwakeSoundPlayer] 的 SoundPool 实现：fire-and-forget，[play] 全程不抛。
 *
 * - 静音遵从：播放前经 [shouldPlay] 裁决——系统铃声非 NORMAL 一律不出声（系统静音优先）；
 *   应用内 soundEnabled 由 init 内后台协程收集（禁 runBlocking，不阻塞构造线程）；
 * - 缺资源 no-op：音频由用户后补，resId 解析不到（<=0）即静默返回，属设计行为；
 * - 音轨缓存：同一事件只 load 一次，重复 play 复用 soundId；
 * - 所有 SoundPool / 系统服务调用都在 runCatching 内，任何底层异常都被吞掉，调用方零防御。
 */
@Singleton
class SoundPoolPlayer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        prefs: UserPreferencesRepository,
    ) : AwakeSoundPlayer {
        @Volatile
        private var enabled: Boolean = true

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val soundPool: SoundPool =
            SoundPool
                .Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                ).build()

        /** event → 已加载 soundId；并发 play 竞争时最坏多 load 一次，无副作用。 */
        private val loadedSoundIds = ConcurrentHashMap<SoundEvent, Int>()

        init {
            scope.launch {
                prefs.soundEnabled.collect { enabled = it }
            }
        }

        override fun play(event: SoundEvent) {
            // 系统静音遵从 + 应用内开关，先于一切 SoundPool 交互。
            if (!shouldPlay(currentRingerMode(), enabled)) return
            runCatching {
                val soundId = resolveSoundId(event) ?: return@runCatching
                soundPool.play(
                    soundId,
                    VOLUME,
                    VOLUME,
                    PLAY_PRIORITY,
                    LOOP_ONCE,
                    playbackRateFor(event, Random.Default),
                )
            }
            // 播放结果不关心：fire-and-forget；runCatching 兜底保证 play 永不抛。
        }

        /** 测试观察位：当前生效的声音开关（后台收集落定后的值）。 */
        internal fun soundEnabledNow(): Boolean = enabled

        /** 测试观察位：暴露内部 SoundPool 供 ShadowSoundPool 断言回放。 */
        internal fun soundPoolForTest(): SoundPool = soundPool

        /** 解析 raw 资源并加载音轨，命中缓存直接复用；资源缺失（resId<=0）返回 null → no-op。 */
        private fun resolveSoundId(event: SoundEvent): Int? {
            loadedSoundIds[event]?.let { return it }
            val resId =
                context.resources.getIdentifier(event.rawName, "raw", context.packageName)
            if (resId <= 0) return null
            val soundId = soundPool.load(context, resId, LOAD_PRIORITY)
            loadedSoundIds[event] = soundId
            return soundId
        }

        private fun currentRingerMode(): Int =
            runCatching {
                (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode
            }.getOrNull() ?: AudioManager.RINGER_MODE_NORMAL

        private companion object {
            /** 同时至多两层声景（如呼噜垫底 + 一声落杯），再多就该安静了。 */
            const val MAX_STREAMS = 2

            /** 全档音量，响度交给系统媒体音量。 */
            const val VOLUME = 1.0f

            /** 不循环：所有音效都是一次性句子。 */
            const val LOOP_ONCE = 0

            const val PLAY_PRIORITY = 1

            const val LOAD_PRIORITY = 1
        }
    }
