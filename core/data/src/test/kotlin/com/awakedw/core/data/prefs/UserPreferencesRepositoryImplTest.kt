package com.awakedw.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** v0.2 画廊与音效契约：临时文件 DataStore 上验证解锁集/钉选/每日选择/音效开关的默认值与读写。 */
class UserPreferencesRepositoryImplTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: UserPreferencesRepositoryImpl

    @Before
    fun setUp() {
        val tmpDir = Files.createTempDirectory("awake_prefs_gallery_test").toFile()
        dataStore =
            PreferenceDataStoreFactory.create(produceFile = { File(tmpDir, "user_prefs_gallery_test.preferences_pb") })
        repo = UserPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun `初始unlockedOutfits为空集`() =
        runTest {
            assertEquals(emptySet<String>(), repo.unlockedOutfits.first())
        }

    @Test
    fun `markOutfitsUnlocked后flow命中_重复标记幂等`() =
        runTest {
            repo.markOutfitsUnlocked(listOf("dress_00"))
            assertEquals(setOf("dress_00"), repo.unlockedOutfits.first())
            repo.markOutfitsUnlocked(listOf("dress_00", "dress_01"))
            assertEquals(setOf("dress_00", "dress_01"), repo.unlockedOutfits.first())
        }

    @Test
    fun `初始unseenOutfits为空集`() =
        runTest {
            assertEquals(emptySet<String>(), repo.unseenOutfits.first())
        }

    @Test
    fun `markOutfitsUnseen后flow命中_重复标记幂等`() =
        runTest {
            repo.markOutfitsUnseen(listOf("dress_01"))
            assertEquals(setOf("dress_01"), repo.unseenOutfits.first())
            repo.markOutfitsUnseen(listOf("dress_01", "dress_02"))
            assertEquals(setOf("dress_01", "dress_02"), repo.unseenOutfits.first())
        }

    @Test
    fun `markOutfitsSeen后从未看集移除_移除不存在id幂等`() =
        runTest {
            repo.markOutfitsUnseen(listOf("dress_01", "dress_02"))
            repo.markOutfitsSeen(listOf("dress_01", "dress_03"))
            assertEquals(setOf("dress_02"), repo.unseenOutfits.first())
            repo.markOutfitsSeen(listOf("dress_02"))
            assertEquals(emptySet<String>(), repo.unseenOutfits.first())
        }

    @Test
    fun `setPinnedOutfit非null命中_置null回落无钉选`() =
        runTest {
            assertNull(repo.pinnedOutfitId.first())
            repo.setPinnedOutfit("dress_01")
            assertEquals("dress_01", repo.pinnedOutfitId.first())
            repo.setPinnedOutfit(null)
            assertNull(repo.pinnedOutfitId.first())
        }

    @Test
    fun `setDailyOutfit后dailyOutfit返回day与outfit对_无记录为null`() =
        runTest {
            assertNull(repo.dailyOutfit())
            repo.setDailyOutfit("2026-08-31", "dress_00")
            assertEquals(Pair("2026-08-31", "dress_00"), repo.dailyOutfit())
        }

    @Test
    fun `soundEnabled默认true_setSoundEnabled生效`() =
        runTest {
            assertEquals(true, repo.soundEnabled.first())
            repo.setSoundEnabled(false)
            assertEquals(false, repo.soundEnabled.first())
        }
}
