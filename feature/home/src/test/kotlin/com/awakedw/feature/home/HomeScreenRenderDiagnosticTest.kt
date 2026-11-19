package com.awakedw.feature.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.LocalUiTimeMillis
import com.awakedw.core.domain.DeleteWaterRecordUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 整页诊断渲染（不是像素基线，不比对）：
 * 以 NATIVE 图形管线 + 减少动态路径拍下真实 `HomeScreen`（含猫栏、真实背景素材与纸面层），
 * 产到 `build/reports/editorial-render/`，供人工检查纸边、页眉页脚、窗环、压印按钮与撕票。
 *
 * 底栏属于 :app 壳，不在本诊断内；Robolectric 的首页像素基线仍受环境限制，
 * 真机走查（八主题、1.3×/2×、TalkBack）才是最终视觉闸门。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenRenderDiagnosticTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var rootView: View

    @After
    fun restoreSystemAnimations() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    @Test
    fun `render populated home on emerald`() = render(ThemeId.EMERALD, "home-emerald")

    @Test
    fun `render populated home on night`() = render(ThemeId.NIGHT, "home-night")

    private fun render(
        themeId: ThemeId,
        label: String,
    ) {
        disableSystemAnimations()
        val nowMillis =
            LocalDateTime.of(2026, 11, 17, 8, 12)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val clock = FakeClock(nowMillis)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
        val viewModel =
            HomeViewModel(
                clock = clock,
                observeHome = ObserveHomeUseCase(water, prefs),
                logWater = LogWaterUseCase(water, prefs, clock),
                deleteWater = DeleteWaterRecordUseCase(water),
                copies = FakeCopyLibraryRepository(catLines = listOf("早安。")),
                sound = FakeSoundPlayer(),
            )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            rootView = LocalView.current
            CompositionLocalProvider(LocalUiTimeMillis provides nowMillis) {
                AwakeTheme(themeId = themeId) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
        advance(100)
        composeRule.runOnIdle { water.seedToday(60, 30) }
        advance(100)
        advance(1_000)

        val image = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        rootView.draw(Canvas(image))
        val output = File("build/reports/editorial-render/$label.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** 减少动态：入场/横幅直接落定，截图稳定；同时覆盖 FadeUpOnce 的无动画路径。 */
    private fun disableSystemAnimations() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    private fun advance(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.mainClock.advanceTimeBy(ms)
    }
}
