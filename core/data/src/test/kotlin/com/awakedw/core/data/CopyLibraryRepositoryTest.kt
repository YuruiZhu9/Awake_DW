package com.awakedw.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.awakedw.core.data.copy.DefaultCopies
import com.awakedw.core.data.copy.DefaultCopyLibraryRepository
import com.awakedw.core.data.copy.ShortCopies
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
    fun `默认文案守则_长度_唯一_无分隔符_无第二三人称_无近似重复`() =
        runTest {
            val lib = repo.library.first()
            val groups = listOf(lib.morning, lib.day, lib.evening)
            val all = groups.flatten()

            assertEquals(108, all.size)
            assertTrue(all.all { it.isNotBlank() })
            assertEquals("全表不得出现重复句", all.size, all.toSet().size)
            assertTrue("长度应落在 10–24 字（含标点），便于问候与通知一行读完", all.all { it.length in 10..24 })
            assertTrue("不得含去重池分隔符 |", all.none { it.contains('|') })

            val forbidden = listOf('你', '您', '她', '他')
            all.forEach { line ->
                forbidden.forEach { ch ->
                    assertFalse("「$line」出现「$ch」，破坏第一人称自述口吻", line.contains(ch))
                }
            }
            assertTrue("每组都要有第一人称锚点", groups.all { group -> group.any { it.contains("我") } })

            // 近似重复是「尴尬」的主要来源：同组内不得有两句共享前六个字。
            groups.forEach { group ->
                val heads = group.map { it.take(6) }
                assertEquals("同组内存在近似重复的起手：$heads", heads.size, heads.toSet().size)
            }
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
    fun `打卡确认与猫语取自内置短句池且与长句去重池各记各的`() =
        runTest {
            val praise = repo.randomPraise(TimeSlot.MORNING)
            val cat = repo.randomCatLine(TimeSlot.MORNING)
            val long = repo.randomFor(TimeSlot.MORNING)

            assertTrue("打卡确认应来自短句池：$praise", praise in ShortCopies.praiseMorning)
            assertTrue("猫语应来自短句池：$cat", cat in ShortCopies.catMorning)
            assertTrue("长句仍来自可编辑文案库：$long", long in DefaultCopies.morning)

            // 三类语料各自持有去重键，互不挤占窗口。
            val raw = dataStore.data.first()[stringPreferencesKey("recent_copy_ids")].orEmpty()
            assertTrue("长句池键应存在", raw.contains("\"MORNING|"))
            assertTrue("打卡确认池键应存在", raw.contains("PRAISE_MORNING|"))
            assertTrue("猫语池键应存在", raw.contains("CAT_MORNING|"))
        }

    @Test
    fun `语义感知：短句池连抽不重复且不受长句池内容影响`() =
        runTest {
            // 把长句全部删空——短句池与应用内的编辑互不影响，仍应正常抽出。
            repeat(36) { repo.delete(TimeSlot.DAY, 0) }
            repeat(6) {
                val praise = repo.randomPraise(TimeSlot.DAY, avoidRecent = 4)
                assertTrue(praise in ShortCopies.praiseDay)
            }
            repeat(6) {
                val cat = repo.randomCatLine(TimeSlot.DAY, avoidRecent = 4)
                assertTrue(cat in ShortCopies.catDay)
            }
            assertTrue("长句组被删空后仍回退默认组", repo.randomFor(TimeSlot.DAY) in DefaultCopies.day)
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
