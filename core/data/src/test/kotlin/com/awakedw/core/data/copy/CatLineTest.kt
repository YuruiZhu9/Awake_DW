package com.awakedw.core.data.copy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.domain.contracts.SHORT_POOL_AVOID_RECENT
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
 * 猫语与打卡确认的语料来源：两者都走内置短句池，与可编辑的心意文案库解耦。
 * 同时保留旧版 cat 字段的 JSON 兼容性——它只作为历史数据被读进来，不再作为活动语料池。
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
    fun `randomCatLine从内置短句池抽取且最近窗口不重复`() =
        runTest {
            val draws = mutableListOf<String>()
            repeat(20) { draws += repo.randomCatLine(TimeSlot.MORNING, avoidRecent = SHORT_POOL_AVOID_RECENT) }
            draws.forEachIndexed { index, text ->
                assertTrue("第${index}抽应来自早间猫语池：$text", text in ShortCopies.catMorning)
                val recentWindow = draws.subList(maxOf(0, index - SHORT_POOL_AVOID_RECENT), index)
                assertFalse("第${index}抽与最近窗口重复", text in recentWindow)
            }
        }

    @Test
    fun `编辑文案库不影响猫语与打卡确认`() =
        runTest {
            // 把早组长句整组换成完全不同的内容。
            dataStore.edit {
                it[libraryJsonKey] =
                    Json.encodeToString(
                        CopyLibrary.serializer(),
                        CopyLibrary(morning = listOf("自定义早句甲", "自定义早句乙"), day = emptyList(), evening = emptyList()),
                    )
            }

            repeat(10) {
                assertTrue(repo.randomCatLine(TimeSlot.MORNING) in ShortCopies.catMorning)
                assertTrue(repo.randomPraise(TimeSlot.MORNING) in ShortCopies.praiseMorning)
            }
            // 问候语仍取使用者编辑后的长句。
            assertTrue(repo.randomFor(TimeSlot.MORNING) in listOf("自定义早句甲", "自定义早句乙"))
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
                assertTrue("应来自内置猫语池：$line", line in ShortCopies.catMorning)
                assertFalse(line == "旧猫句")
            }
        }

    @Test
    fun `旧格式JSON缺cat字段仍可抽取当前时段文案`() =
        runTest {
            dataStore.edit { it[libraryJsonKey] = """{"morning":["旧库早句"],"day":[],"evening":[]}""" }
            assertEquals("旧库早句", repo.randomFor(TimeSlot.MORNING, avoidRecent = 0))
        }

    @Test
    fun `长句池被删空时回退对应默认组`() =
        runTest {
            dataStore.edit { it[libraryJsonKey] = """{"morning":[],"day":[],"evening":[]}""" }
            repeat(10) {
                assertTrue(repo.randomFor(TimeSlot.MORNING) in DefaultCopies.morning)
            }
        }

    @Test
    fun `三类语料各自持有去重键互不挤占`() =
        runTest {
            repo.randomFor(TimeSlot.MORNING, avoidRecent = 0)
            repo.randomPraise(TimeSlot.MORNING, avoidRecent = 0)
            repo.randomCatLine(TimeSlot.MORNING, avoidRecent = 0)

            val raw = dataStore.data.first()[recentIdsKey].orEmpty()
            assertTrue("长句键", raw.contains("\"MORNING|"))
            assertTrue("打卡确认键", raw.contains("PRAISE_MORNING|"))
            assertTrue("猫语键", raw.contains("CAT_MORNING|"))
            assertFalse("当前版本不应再写入隐藏 CAT 组", raw.contains("\"CAT|"))
        }

    @Test
    fun `resetToDefaults清空全部去重池`() =
        runTest {
            repo.randomFor(TimeSlot.MORNING, avoidRecent = 0)
            repo.randomPraise(TimeSlot.MORNING, avoidRecent = 0)
            repo.resetToDefaults()

            assertEquals(null, dataStore.data.first()[recentIdsKey])
            assertEquals(DefaultCopies.morning, repo.library.first().morning)
        }
}
