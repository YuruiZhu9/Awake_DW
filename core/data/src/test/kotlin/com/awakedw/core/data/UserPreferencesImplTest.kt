package com.awakedw.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.awakedw.core.data.prefs.UserPreferencesRepositoryImpl
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** 用户设置仓储：临时文件 DataStore 上验证默认值、set 回流与键名契约。 */
class UserPreferencesImplTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: UserPreferencesRepositoryImpl

    @Before
    fun setUp() {
        val tmpDir = Files.createTempDirectory("awake_prefs_test").toFile()
        dataStore =
            PreferenceDataStoreFactory.create(produceFile = { File(tmpDir, "user_prefs_test.preferences_pb") })
        repo = UserPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun `默认值_流首帧等于UserSettings默认_onboarding未完成_无celebrated`() =
        runTest {
            assertEquals(UserSettings(), repo.settings.first())
            assertEquals(false, repo.onboardingDone())
            assertNull(repo.celebratedDayKey())
        }

    @Test
    fun `set后回流新值`() =
        runTest {
            repo.setGoalMl(2000)
            repo.setCupMl(300)
            repo.setWindow(420, 1320)
            repo.setIntervalMin(60)
            repo.setRemindersEnabled(false)
            repo.setThemeChoice(ThemeChoice.FIXED_STRAWBERRY)

            assertEquals(
                UserSettings(
                    goalMl = 2000,
                    cupMl = 300,
                    windowStartMin = 420,
                    windowEndMin = 1320,
                    intervalMin = 60,
                    remindersEnabled = false,
                    themeChoice = ThemeChoice.FIXED_STRAWBERRY,
                ),
                repo.settings.first(),
            )
        }

    @Test
    fun `键名契约_按设计存储_主题以枚举名持久化`() =
        runTest {
            repo.setGoalMl(1)
            repo.setCupMl(2)
            repo.setWindow(3, 4)
            repo.setIntervalMin(5)
            repo.setRemindersEnabled(false)
            repo.setThemeChoice(ThemeChoice.FIXED_CARAMEL)
            repo.markOnboardingDone()
            repo.markCelebrated("2026-08-27")

            val prefs = dataStore.data.first()
            val names = prefs.asMap().keys.map { it.name }.toSet()
            assertTrue(
                names.containsAll(
                    listOf(
                        "goal_ml",
                        "cup_ml",
                        "window_start_min",
                        "window_end_min",
                        "interval_min",
                        "reminders_enabled",
                        "theme_mode",
                        "onboarding_done",
                        "celebrated_day_key",
                    ),
                ),
            )
            assertEquals("FIXED_CARAMEL", prefs[stringPreferencesKey("theme_mode")])
            assertEquals(1, prefs[intPreferencesKey("goal_ml")])
        }

    @Test
    fun `markCelebrated返回写入值_setWindow边界可用`() =
        runTest {
            repo.markCelebrated("2026-08-28")
            assertEquals("2026-08-28", repo.celebratedDayKey())
            repo.markCelebrated("2026-09-01")
            assertEquals("2026-09-01", repo.celebratedDayKey())

            repo.setWindow(0, 1439)
            val s = repo.settings.first()
            assertEquals(0, s.windowStartMin)
            assertEquals(1439, s.windowEndMin)
        }

    @Test
    fun `未知主题名读取时回退FOLLOW_TIME`() =
        runTest {
            dataStore.edit { it[stringPreferencesKey("theme_mode")] = "NOT_A_CHOICE" }
            assertEquals(ThemeChoice.FOLLOW_TIME, repo.settings.first().themeChoice)
        }
}
