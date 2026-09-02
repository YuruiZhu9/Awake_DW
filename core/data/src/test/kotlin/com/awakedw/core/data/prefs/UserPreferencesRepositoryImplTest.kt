package com.awakedw.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.awakedw.core.model.ThemeChoice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** User preference persistence for water tracking and presentation settings. */
class UserPreferencesRepositoryImplTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: UserPreferencesRepositoryImpl

    @Before
    fun setUp() {
        val tmpDir = Files.createTempDirectory("awake_prefs_visual_test").toFile()
        dataStore = PreferenceDataStoreFactory.create(produceFile = { File(tmpDir, "user_prefs.preferences_pb") })
        repo = UserPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun `sound is enabled by default and can be persisted`() =
        runTest {
            assertEquals(true, repo.soundEnabled.first())
            repo.setSoundEnabled(false)
            assertEquals(false, repo.soundEnabled.first())
        }

    @Test
    fun `water settings and theme choice flow back from the store`() =
        runTest {
            repo.setGoalMl(2000)
            repo.setCupMl(300)
            repo.setWindow(420, 1320)
            repo.setIntervalMin(60)
            repo.setRemindersEnabled(false)
            repo.setThemeChoice(ThemeChoice.FIXED_STRAWBERRY)

            val settings = repo.settings.first()
            assertEquals(2000, settings.goalMl)
            assertEquals(300, settings.cupMl)
            assertEquals(420, settings.windowStartMin)
            assertEquals(1320, settings.windowEndMin)
            assertEquals(60, settings.intervalMin)
            assertEquals(false, settings.remindersEnabled)
            assertEquals(ThemeChoice.FIXED_STRAWBERRY, settings.themeChoice)
            assertNull(repo.celebratedDayKey())
        }
}
