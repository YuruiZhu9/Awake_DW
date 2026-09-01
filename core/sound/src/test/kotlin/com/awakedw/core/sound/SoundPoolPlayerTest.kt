package com.awakedw.core.sound

import android.content.Context
import android.media.AudioManager
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * SoundPoolPlayer 契约：微变调速率为纯函数可断言；play fire-and-forget 绝不抛；
 * 缺 raw 资源 no-op；soundEnabled=false 与系统静音（ringerMode≠NORMAL）一律不发声；
 * 放行时确实把（带速率的）播放指令送达 SoundPool（Robolectric ShadowSoundPool 记录回放）。
 *
 * 测试用 res/raw 仅含 drop_a.wav：供放行/守卫/速率回放断言；其余事件在测试环境同样缺资源，
 * 正好覆盖「resId<=0 → no-op」分支。生产环境全部缺资源由用户后补音频。
 *
 * 显式钉 SDK 35：库模块的 Robolectric 默认落在 minSdk=26，与生产目标不符。
 */
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class SoundPoolPlayerTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    // —— playbackRateFor 纯函数 ——

    @Test
    fun `掉落音速率始终落在微变调区间`() {
        val random = Random(42)
        repeat(1_000) {
            for (drop in listOf(SoundEvent.DROP_A, SoundEvent.DROP_B, SoundEvent.DROP_C)) {
                val rate = playbackRateFor(drop, random)
                assertTrue("rate=$rate 越界", rate in DROP_RATE_MIN..DROP_RATE_MAX)
            }
        }
    }

    @Test
    fun `掉落音速率随机数取零时恰为下界`() {
        assertEquals(DROP_RATE_MIN, playbackRateFor(SoundEvent.DROP_A, FixedRandom(0f)))
    }

    @Test
    fun `目标达成旋律速率恒为一`() {
        repeat(100) {
            assertEquals(1.0f, playbackRateFor(SoundEvent.GOAL_MELODY, FixedRandom(0.5f)))
        }
    }

    @Test
    fun `呼噜声速率恒为固定的低沉值`() {
        assertEquals(PURR_RATE, playbackRateFor(SoundEvent.PURR, FixedRandom(0.5f)))
    }

    // —— play 行为（Robolectric 真实资源 + ShadowSoundPool） ——

    @Test
    fun `无对应raw资源时play静默不抛`() {
        val player = SoundPoolPlayer(context, FakeUserPrefs(soundEnabled = true))
        // 模块未随附任何音频资源：缺资源 no-op 是设计行为，绝不抛。
        player.play(SoundEvent.GOAL_MELODY)
        player.play(SoundEvent.PURR)
        player.play(SoundEvent.DROP_B)
        val shadow = shadowOf(player.soundPoolForTest())
        assertFalse(shadow.wasResourcePlayed(rawResId("goal_melody")))
    }

    @Test
    fun `系统铃声为静音时play不发声`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        val player = SoundPoolPlayer(context, FakeUserPrefs(soundEnabled = true))
        awaitUntil { player.soundEnabledNow() }
        player.play(SoundEvent.DROP_A)
        assertFalse(shadowOf(player.soundPoolForTest()).wasResourcePlayed(rawResId("drop_a")))
    }

    @Test
    fun `系统铃声为振动时play不发声`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        val player = SoundPoolPlayer(context, FakeUserPrefs(soundEnabled = true))
        awaitUntil { player.soundEnabledNow() }
        player.play(SoundEvent.DROP_A)
        assertFalse(shadowOf(player.soundPoolForTest()).wasResourcePlayed(rawResId("drop_a")))
    }

    @Test
    fun `用户关闭音效时play不发声`() {
        val player = SoundPoolPlayer(context, FakeUserPrefs(soundEnabled = false))
        awaitUntil { !player.soundEnabledNow() }
        player.play(SoundEvent.DROP_A)
        assertFalse(shadowOf(player.soundPoolForTest()).wasResourcePlayed(rawResId("drop_a")))
    }

    @Test
    fun `正常铃声且开启音效时play送达SoundPool且速率在微变调区间`() {
        val player = SoundPoolPlayer(context, FakeUserPrefs(soundEnabled = true))
        awaitUntil { player.soundEnabledNow() }
        player.play(SoundEvent.DROP_A)
        val resId = rawResId("drop_a")
        val shadow = shadowOf(player.soundPoolForTest())
        assertTrue("应向 SoundPool 下发播放指令", shadow.wasResourcePlayed(resId))
        val rate = shadow.getResourcePlaybacks(resId).single().rate
        assertTrue("回放速率 rate=$rate 越界", rate in DROP_RATE_MIN..DROP_RATE_MAX)
    }

    @Test
    fun `重复play同一事件复用已加载音轨`() {
        val player = SoundPoolPlayer(context, FakeUserPrefs(soundEnabled = true))
        awaitUntil { player.soundEnabledNow() }
        player.play(SoundEvent.DROP_A)
        player.play(SoundEvent.DROP_A)
        assertEquals(2, shadowOf(player.soundPoolForTest()).getResourcePlaybacks(rawResId("drop_a")).size)
    }

    // —— 测试脚手架 ——

    private fun rawResId(name: String): Int = context.resources.getIdentifier(name, "raw", context.packageName)

    /** 生产侧收集在 Dispatchers.Default 协程里生效，测试轮询等待落定（不用 runBlocking 污染生产代码）。 */
    private fun awaitUntil(
        timeoutMs: Long = 5_000,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "等待 SoundPoolPlayer 状态落定超时" }
            Thread.sleep(10)
        }
    }

    /** 固定输出随机源：速率区间边界与固定值断言用。 */
    private class FixedRandom(private val value: Float) : Random() {
        override fun nextBits(bitCount: Int): Int = 0

        override fun nextFloat(): Float = value
    }

    /** 仅 soundEnabled 真实；其余契约成员本测试绝不触及。 */
    private class FakeUserPrefs(soundEnabled: Boolean) : UserPreferencesRepository {
        private val sound = MutableStateFlow(soundEnabled)

        override val soundEnabled: StateFlow<Boolean> = sound.asStateFlow()

        override val settings: Flow<UserSettings> get() = throw UnsupportedOperationException()

        override suspend fun setGoalMl(v: Int) = throw UnsupportedOperationException()

        override suspend fun setCupMl(v: Int) = throw UnsupportedOperationException()

        override suspend fun setWindow(
            startMin: Int,
            endMin: Int,
        ) = throw UnsupportedOperationException()

        override suspend fun setIntervalMin(v: Int) = throw UnsupportedOperationException()

        override suspend fun setRemindersEnabled(v: Boolean) = throw UnsupportedOperationException()

        override suspend fun setThemeChoice(v: ThemeChoice) = throw UnsupportedOperationException()

        override suspend fun markCelebrated(dayKey: String) = throw UnsupportedOperationException()

        override suspend fun celebratedDayKey(): String? = throw UnsupportedOperationException()

        override suspend fun markOnboardingDone() = throw UnsupportedOperationException()

        override suspend fun onboardingDone(): Boolean = throw UnsupportedOperationException()

        override val unlockedOutfits: Flow<Set<String>> get() = throw UnsupportedOperationException()

        override suspend fun markOutfitsUnlocked(ids: Collection<String>) = throw UnsupportedOperationException()

        override val pinnedOutfitId: Flow<String?> get() = throw UnsupportedOperationException()

        override suspend fun setPinnedOutfit(id: String?) = throw UnsupportedOperationException()

        override suspend fun dailyOutfit(): Pair<String, String>? = throw UnsupportedOperationException()

        override suspend fun setDailyOutfit(
            dayKey: String,
            outfitId: String,
        ) = throw UnsupportedOperationException()

        override suspend fun setSoundEnabled(v: Boolean) = throw UnsupportedOperationException()
    }
}
