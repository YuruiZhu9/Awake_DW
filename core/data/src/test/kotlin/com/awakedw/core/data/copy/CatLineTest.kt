package com.awakedw.core.data.copy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.model.TimeSlot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 胆大王交互语料：验证它与「心意文案库」共享当前时段来源，
 * 同时保留旧版 cat 字段的 JSON 兼容性而不再读取旧猫语池。
 */
class CatLineTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: DefaultCopyLibraryRepository

    @Before
    fun setUp() {
        val tmpDir = Files.createTempDirectory("awake_cat_test").toFile()
        dataStore = PreferenceDataStoreFactory.create(produceFile = { File(tmpDir, "cat_line_test.preferences_pb") })
        repo = DefaultCopyLibraryRepository(dataStore)
    }

    private val libraryJsonKey = stringPreferencesKey(CopyPrefKeys.COPY_LIBRARY_JSON)
    private val recentIdsKey = stringPreferencesKey(CopyPrefKeys.RECENT_COPY_IDS)

    @Test
    fun `默认心意文案108句且每个时段36句`() {
        assertEquals(36, DefaultCopies.morning.size)
        assertEquals(36, DefaultCopies.day.size)
        assertEquals(36, DefaultCopies.evening.size)
        assertTrue(DefaultCopies.morning.all { it.isNotBlank() })
        assertTrue(DefaultCopies.day.all { it.isNotBlank() })
        assertTrue(DefaultCopies.evening.all { it.isNotBlank() })
        assertTrue(DefaultCopies.morning.none { it.contains('|') })
    }

    @Test
    fun `randomCatLine从当前时段心意文案抽取且最近窗口不重复`() =
        runTest {
            val customMorning = listOf("晨句甲", "晨句乙", "晨句丙", "晨句丁", "晨句戊", "晨句己")
            dataStore.edit {
                it[libraryJsonKey] =
                    Json.encodeToString(
                        CopyLibrary.serializer(),
                        CopyLibrary(
                            morning = customMorning,
                            day = emptyList(),
                            evening = emptyList(),
                        ),
                    )
            }

            val draws = mutableListOf<String>()
            repeat(20) { draws += repo.randomCatLine(TimeSlot.MORNING, avoidRecent = 5) }
            draws.forEachIndexed { index, text ->
                assertTrue("第${index}抽应来自早安心意文案：$text", text in customMorning)
                val recentWindow = draws.subList(maxOf(0, index - 5), index)
                assertFalse("第${index}抽与最近5条窗口重复", text in recentWindow)
            }
        }

    @Test
    fun `旧版cat字段可反序列化但不再作为活动语料`() =
        runTest {
            dataStore.edit {
                it[libraryJsonKey] =
                    """{"morning":["当前早句甲","当前早句乙"],"day":[],"evening":[],"cat":["旧猫句"]}"""
            }
            repeat(10) {
                val line = repo.randomCatLine(TimeSlot.MORNING, avoidRecent = 0)
                assertTrue(line == "当前早句甲" || line == "当前早句乙")
                assertFalse(line == "旧猫句")
            }
        }

    @Test
    fun `旧格式JSON缺cat字段仍可抽取当前时段文案`() =
        runTest {
            dataStore.edit { it[libraryJsonKey] = """{"morning":["旧库早句"],"day":[],"evening":[]}""" }
            assertEquals("旧库早句", repo.randomCatLine(TimeSlot.MORNING, avoidRecent = 0))
        }

    @Test
    fun `当前时段心意文案为空时回退对应默认组`() =
        runTest {
            dataStore.edit { it[libraryJsonKey] = """{"morning":[],"day":[],"evening":[]}""" }
            repeat(10) {
                assertTrue(repo.randomCatLine(TimeSlot.MORNING) in DefaultCopies.morning)
            }
        }

    @Test
    fun `猫交互与普通反馈共享时段去重池`() =
        runTest {
            dataStore.edit { it[recentIdsKey] = "[\"MORNING|早句\"]" }
            repo.randomCatLine(TimeSlot.MORNING, avoidRecent = 0)
            repo.randomFor(TimeSlot.MORNING, avoidRecent = 0)

            val raw = dataStore.data.first()[recentIdsKey]
            assertTrue(raw.orEmpty().contains("MORNING|"))
            assertFalse("当前版本不应再写入隐藏 CAT 组", raw.orEmpty().contains("CAT|"))
        }
}
