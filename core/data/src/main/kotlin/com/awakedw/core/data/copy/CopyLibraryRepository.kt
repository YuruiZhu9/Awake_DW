package com.awakedw.core.data.copy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.awakedw.core.domain.contracts.CopyLibrary
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.model.TimeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** DataStore 键名契约（设计 §5.3）。 */
internal object CopyPrefKeys {
    const val COPY_LIBRARY_JSON = "copy_library_json"

    /**
     * 去重池持久化格式：JSON 字符串数组，每项为 `"<池键>|<句子>"`。
     * 例：`["MORNING|清晨刚亮…", "PRAISE_MORNING|记好了，清晨慢慢来", "CAT_DAY|喵~"]`。
     *
     * 池键：长句用时段名（`MORNING`/`DAY`/`EVENING`，与历史数据同名，向后兼容），
     * 打卡确认与猫咪回应用带前缀的键，三类语料各自独立去重、互不挤占窗口。
     */
    const val RECENT_COPY_IDS = "recent_copy_ids"
}

/** Preferences DataStore 实现：库与去重池都以 JSON 字符串存于单键。 */
class DefaultCopyLibraryRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : CopyLibraryRepository {
        private val json = Json { ignoreUnknownKeys = true }
        private val selectionMutex = Mutex()

        private val copyLibraryJsonKey = stringPreferencesKey(CopyPrefKeys.COPY_LIBRARY_JSON)
        private val recentCopyIdsKey = stringPreferencesKey(CopyPrefKeys.RECENT_COPY_IDS)

        override val library: Flow<CopyLibrary> =
            dataStore.data.map { prefs -> decodeLibrary(prefs[copyLibraryJsonKey]) }

        override suspend fun randomFor(
            slot: TimeSlot,
            avoidRecent: Int,
        ): String =
            selectionMutex.withLock {
                val prefs = dataStore.data.first()
                // 组被删空时回退对应默认时段，保证永远有句子可返回。
                val customPool = decodeLibrary(prefs[copyLibraryJsonKey]).groupOf(slot)
                val pool = if (customPool.isEmpty()) DefaultCopies.groupOf(slot) else customPool
                drawAndPersist(
                    pool = pool,
                    recentsByKey = decodeRecents(prefs[recentCopyIdsKey]),
                    key = slot.name,
                    avoidRecent = avoidRecent,
                )
            }

        override suspend fun randomPraise(
            slot: TimeSlot,
            avoidRecent: Int,
        ): String =
            selectionMutex.withLock {
                val prefs = dataStore.data.first()
                drawAndPersist(
                    pool = ShortCopies.praiseOf(slot),
                    recentsByKey = decodeRecents(prefs[recentCopyIdsKey]),
                    key = PRAISE_KEY_PREFIX + slot.name,
                    avoidRecent = avoidRecent,
                )
            }

        override suspend fun randomCatLine(
            slot: TimeSlot,
            avoidRecent: Int,
        ): String =
            selectionMutex.withLock {
                val prefs = dataStore.data.first()
                drawAndPersist(
                    pool = ShortCopies.catOf(slot),
                    recentsByKey = decodeRecents(prefs[recentCopyIdsKey]),
                    key = CAT_KEY_PREFIX + slot.name,
                    avoidRecent = avoidRecent,
                )
            }

        override suspend fun upsert(
            slot: TimeSlot,
            index: Int,
            text: String,
        ) {
            dataStore.edit { prefs ->
                val lib = decodeLibrary(prefs[copyLibraryJsonKey])
                prefs[copyLibraryJsonKey] = json.encodeToString(lib.withUpserted(slot, index, text))
            }
        }

        override suspend fun delete(
            slot: TimeSlot,
            index: Int,
        ) {
            dataStore.edit { prefs ->
                val lib = decodeLibrary(prefs[copyLibraryJsonKey])
                prefs[copyLibraryJsonKey] = json.encodeToString(lib.withDeleted(slot, index))
            }
        }

        override suspend fun resetToDefaults() {
            dataStore.edit {
                it.remove(copyLibraryJsonKey)
                it.remove(recentCopyIdsKey)
            }
        }

        /**
         * 从 [pool] 抽一句并写回 [key] 的去重池：跳过最近 [avoidRecent] 条，
         * 候选耗尽即清空该池重来（保证永远抽得出）。
         */
        private suspend fun drawAndPersist(
            pool: List<String>,
            recentsByKey: Map<String, List<String>>,
            key: String,
            avoidRecent: Int,
        ): String {
            if (pool.isEmpty()) error("文案池为空：$key")
            val recents = recentsByKey[key].orEmpty()
            val blocked = recents.takeLast(avoidRecent.coerceAtLeast(0)).toSet()
            val candidates = pool.filterNot { it in blocked }
            val chosen: String
            val nextRecents: List<String>
            if (candidates.isEmpty()) {
                chosen = pool.random()
                nextRecents = listOf(chosen)
            } else {
                chosen = candidates.random()
                nextRecents = (recents + chosen).takeLast(RECENT_KEEP_PER_KEY)
            }
            persistRecents(recentsByKey, key, nextRecents)
            return chosen
        }

        private suspend fun persistRecents(
            current: Map<String, List<String>>,
            key: String,
            next: List<String>,
        ) {
            dataStore.edit { prefs ->
                val updated = current.toMutableMap()
                updated[key] = next
                prefs[recentCopyIdsKey] = json.encodeToString(entriesFromRecents(updated))
            }
        }

        private fun CopyLibrary.withUpserted(
            slot: TimeSlot,
            index: Int,
            text: String,
        ): CopyLibrary {
            val list = groupOf(slot).toMutableList()
            if (index in list.indices) list[index] = text else list += text
            return withGroup(slot, list)
        }

        private fun CopyLibrary.withDeleted(
            slot: TimeSlot,
            index: Int,
        ): CopyLibrary {
            val list = groupOf(slot).toMutableList()
            if (index !in list.indices) return this
            list.removeAt(index)
            return withGroup(slot, list)
        }

        private fun CopyLibrary.withGroup(
            slot: TimeSlot,
            values: List<String>,
        ): CopyLibrary =
            when (slot) {
                TimeSlot.MORNING -> copy(morning = values)
                TimeSlot.DAY -> copy(day = values)
                TimeSlot.EVENING -> copy(evening = values)
            }

        private fun decodeLibrary(raw: String?): CopyLibrary =
            raw?.let {
                runCatching { json.decodeFromString<CopyLibrary>(it) }.getOrDefault(defaultLibrary())
            } ?: defaultLibrary()

        private fun defaultLibrary(): CopyLibrary =
            CopyLibrary(morning = DefaultCopies.morning, day = DefaultCopies.day, evening = DefaultCopies.evening)

        /** 存储格式 `["<池键>|句子", …]` ↔ `Map<池键, 最近句列表>`。 */
        private fun entriesFromRecents(recentsByKey: Map<String, List<String>>): List<String> =
            recentsByKey.flatMap { (key, texts) -> texts.map { "$key$ENTRY_SEPARATOR$it" } }

        private fun decodeRecents(raw: String?): Map<String, List<String>> {
            val entries =
                raw?.let {
                    runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
                } ?: emptyList()
            return entries.groupBy(
                keySelector = { entry -> entry.substringBefore(ENTRY_SEPARATOR, missingDelimiterValue = "") },
                valueTransform = { entry -> entry.substringAfter(ENTRY_SEPARATOR, missingDelimiterValue = "") },
            ).mapValues { (_, texts) -> texts.filter { it.isNotEmpty() } }
        }

        private companion object {
            const val ENTRY_SEPARATOR = "|"

            /** 每个池最多保留的最近句子条数（须不小于常用 avoidRecent）。 */
            const val RECENT_KEEP_PER_KEY = 32

            const val PRAISE_KEY_PREFIX = "PRAISE_"
            const val CAT_KEY_PREFIX = "CAT_"
        }
    }
