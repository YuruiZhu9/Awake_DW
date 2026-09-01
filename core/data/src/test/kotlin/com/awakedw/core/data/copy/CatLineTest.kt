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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** 胆大王猫语料组：旧版 JSON 缺 cat 字段的序列化兼容、独立去重池抽取与组空回退。 */
class CatLineTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: DefaultCopyLibraryRepository

    @Before
    fun setUp() {
        val tmpDir = Files.createTempDirectory("awake_cat_test").toFile()
        dataStore =
            PreferenceDataStoreFactory.create(produceFile = { File(tmpDir, "cat_line_test.preferences_pb") })
        repo = DefaultCopyLibraryRepository(dataStore)
    }

    private val libraryJsonKey = stringPreferencesKey("copy_library_json")
    private val recentIdsKey = stringPreferencesKey("recent_copy_ids")

    @Test
    fun `默认猫语料20句且逐句非空`() {
        assertEquals(20, DefaultCopies.cat.size)
        assertTrue(DefaultCopies.cat.all { it.isNotBlank() })
        assertEquals(DefaultCopies.cat.size, DefaultCopies.cat.toSet().size)
    }

    @Test
    fun `旧格式JSON缺cat字段_反序列化成功且cat回落空列表`() {
        val lib = Json.decodeFromString<CopyLibrary>("{\"morning\":[\"a\"],\"day\":[],\"evening\":[]}")
        assertEquals(listOf("a"), lib.morning)
        assertTrue(lib.cat.isEmpty())
    }

    @Test
    fun `含cat字段的JSON往返序列化后保留`() {
        val original = CopyLibrary(morning = listOf("a"), day = emptyList(), evening = emptyList(), cat = listOf("喵一", "喵二"))
        val decoded = Json.decodeFromString<CopyLibrary>(Json.encodeToString(CopyLibrary.serializer(), original))
        assertEquals(original, decoded)
    }

    @Test
    fun `randomCatLine从持久化cat组抽取且20次连抽窗口内不重复`() =
        runTest {
            val catJson = """{"morning":["a"],"day":[],"evening":[],"cat":["喵甲","喵乙","喵丙","喵丁","喵戊","喵己"]}"""
            dataStore.edit { it[libraryJsonKey] = catJson }

            val pool = listOf("喵甲", "喵乙", "喵丙", "喵丁", "喵戊", "喵己")
            val draws = mutableListOf<String>()
            repeat(20) { draws += repo.randomCatLine(avoidRecent = 5) }

            draws.forEachIndexed { i, text ->
                assertTrue("第${i}抽应属于猫组：$text", text in pool)
                val recentWindow = draws.subList(maxOf(0, i - 5), i)
                assertFalse("第${i}抽与最近5条窗口重复", text in recentWindow)
            }
        }

    @Test
    fun `cat组为空时回退默认组且不抛异常`() =
        runTest {
            // 无持久化库 => cat 为空 => 回退默认猫语组。
            val fallback = repo.randomCatLine()
            assertTrue("应回退默认猫语组：$fallback", fallback in DefaultCopies.cat)

            // 持久化库存在但 cat 组被删空 => 同样回退默认组。
            dataStore.edit { it[libraryJsonKey] = """{"morning":["a"],"day":[],"evening":[],"cat":[]}""" }
            repeat(10) {
                val line = repo.randomCatLine()
                assertTrue("应回退默认猫语组：$line", line in DefaultCopies.cat)
            }
        }

    @Test
    fun `旧格式JSON落库后randomCatLine仍回退默认组`() =
        runTest {
            dataStore.edit { it[libraryJsonKey] = """{"morning":["旧库早句"],"day":[],"evening":[]}""" }
            repeat(5) {
                val line = repo.randomCatLine()
                assertTrue("旧库缺cat应回退默认组：$line", line in DefaultCopies.cat)
            }
        }

    @Test
    fun `猫语去重池独立于时段组_不污染MORNING去重记录`() =
        runTest {
            dataStore.edit { it[recentIdsKey] = """["MORNING|早句"]""" }

            repeat(3) { repo.randomCatLine() }
            repo.randomFor(TimeSlot.MORNING)

            val raw = dataStore.data.first()[recentIdsKey]
            val entries = Json.decodeFromString(ListSerializer(String.serializer()), raw!!)
            assertTrue("MORNING 原去重记录应保留", "MORNING|早句" in entries)
            assertTrue("猫语抽句应写入独立的 CAT 组", entries.any { it.startsWith("CAT|") })
        }
}
