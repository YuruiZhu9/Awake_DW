package com.awakedw.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.awakedw.core.data.copy.DefaultCopies
import com.awakedw.core.data.copy.DefaultCopyLibraryRepository
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.model.TimeSlot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** 文案库仓储：默认 108 条、去重抽取窗口、删除兜底与重置。 */
class CopyLibraryRepositoryTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: DefaultCopyLibraryRepository

    @Before
    fun setUp() {
        val tmpDir = Files.createTempDirectory("awake_copy_test").toFile()
        dataStore =
            PreferenceDataStoreFactory.create(produceFile = { File(tmpDir, "copy_lib_test.preferences_pb") })
        repo = DefaultCopyLibraryRepository(dataStore)
    }

    @Test
    fun `默认文案108条且早午晚各36条`() =
        runTest {
            val lib = repo.library.first()
            assertEquals(CopyLibrary(DefaultCopies.morning, DefaultCopies.day, DefaultCopies.evening), lib)
            assertEquals(108, lib.morning.size + lib.day.size + lib.evening.size)
            assertEquals(36, lib.morning.size)
            assertEquals(36, lib.day.size)
            assertEquals(36, lib.evening.size)
        }

    @Test
    fun `randomFor返回早组句子且20次连抽不与最近5条窗口内重复`() =
        runTest {
            val draws = mutableListOf<String>()
            repeat(20) { draws += repo.randomFor(TimeSlot.MORNING, avoidRecent = 5) }

            draws.forEachIndexed { i, text ->
                assertTrue("第${i}抽应属于早组：$text", text in DefaultCopies.morning)
                val recentWindow = draws.subList(maxOf(0, i - 5), i)
                assertFalse("第${i}抽与最近5条窗口重复", text in recentWindow)
            }
        }

    @Test
    fun `删除到剩少数时不抛异常且回退默认组`() =
        runTest {
            // 池从 36 删到 2（小于 avoidRecent=5 的窗口）：候选可能被近期记录耗尽，需清空去重池后仍能返回。
            repeat(34) { repo.delete(TimeSlot.DAY, 0) }
            assertEquals(2, repo.library.first().day.size)
            val shrunkenDraws = mutableListOf<String>()
            repeat(10) { shrunkenDraws += repo.randomFor(TimeSlot.DAY, avoidRecent = 5) }
            assertTrue(shrunkenDraws.isNotEmpty())

            // 全删光：回退任一默认组句子，且不抛异常。
            repeat(2) { repo.delete(TimeSlot.DAY, 0) }
            assertTrue(repo.library.first().day.isEmpty())
            val fallback = repo.randomFor(TimeSlot.DAY)
            assertTrue(fallback in DefaultCopies.day)
        }

    @Test
    fun `删除后的句子不再被抽到`() =
        runTest {
            val removed = repo.library.first().morning.first()
            repo.delete(TimeSlot.MORNING, 0)
            repeat(36) { assertNotEquals(removed, repo.randomFor(TimeSlot.MORNING)) }
        }

    @Test
    fun `upsert替换并追加_reset恢复默认`() =
        runTest {
            repo.upsert(TimeSlot.EVENING, 0, "自定义晚安句")
            var lib = repo.library.first()
            assertEquals("自定义晚安句", lib.evening[0])
            assertEquals(36, lib.evening.size)

            repo.upsert(TimeSlot.EVENING, lib.evening.size, "追加的夜句")
            lib = repo.library.first()
            assertEquals(37, lib.evening.size)
            assertEquals("追加的夜句", lib.evening.last())

            repo.resetToDefaults()
            assertEquals(
                CopyLibrary(DefaultCopies.morning, DefaultCopies.day, DefaultCopies.evening),
                repo.library.first(),
            )
        }

    @Test
    fun `文案库持久化于copy_library_json键`() =
        runTest {
            repo.upsert(TimeSlot.EVENING, 3, "存储键契约校验句")
            val names = dataStore.data.first().asMap().keys.map { it.name }
            assertTrue("应有 copy_library_json 键", names.contains("copy_library_json"))
        }
}
