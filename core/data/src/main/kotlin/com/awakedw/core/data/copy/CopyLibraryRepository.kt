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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** DataStore 键名契约（设计 §5.3）。 */
internal object CopyPrefKeys {
    const val COPY_LIBRARY_JSON = "copy_library_json"

    /**
     * 去重池持久化格式：JSON 字符串数组，每项为 `"<时段名>|<句子>"`。
     * 例：`["MORNING|早安，先喝一口水", "DAY|午后啦…"]`。
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

        private val copyLibraryJsonKey = stringPreferencesKey(CopyPrefKeys.COPY_LIBRARY_JSON)
        private val recentCopyIdsKey = stringPreferencesKey(CopyPrefKeys.RECENT_COPY_IDS)

        override val library: Flow<CopyLibrary> =
            dataStore.data.map { prefs -> decodeLibrary(prefs[copyLibraryJsonKey]) }

        override suspend fun randomFor(
            slot: TimeSlot,
            avoidRecent: Int,
        ): String {
            val prefs = dataStore.data.first()
            val recentsBySlot = decodeRecents(prefs[recentCopyIdsKey])
            val recents = recentsBySlot[slot.name].orEmpty()

            // 组被删空时回退默认组兜底，保证永远有句子可返回。
            val customPool = decodeLibrary(prefs[copyLibraryJsonKey]).groupOf(slot)
            val pool = if (customPool.isEmpty()) DefaultCopies.groupOf(slot) else customPool
            if (pool.isEmpty()) error("文案池为空：$slot")

            // 跳过最近 avoidRecent 条；候选耗尽 => 清空该时段去重池重来。
            val blocked = recents.takeLast(avoidRecent.coerceAtLeast(0)).toSet()
            val candidates = pool.filterNot { it in blocked }
            val chosen: String
            val nextRecents: List<String>
            if (candidates.isEmpty()) {
                chosen = pool.random()
                nextRecents = listOf(chosen)
            } else {
                chosen = candidates.random()
                nextRecents = (recents + chosen).takeLast(RECENT_KEEP_PER_SLOT)
            }

            persistRecents(recentsBySlot, slot.name, nextRecents)
            return chosen
        }

        override suspend fun randomCatLine(avoidRecent: Int): String {
            val prefs = dataStore.data.first()
            val recentsByGroup = decodeRecents(prefs[recentCopyIdsKey])
            val recents = recentsByGroup[GROUP_CAT].orEmpty()

            // 持久化组为空（含旧版库缺 cat 字段）时回退默认猫语组，保证永远有句子可返回。
            val customPool = decodeLibrary(prefs[copyLibraryJsonKey]).cat
            val pool = if (customPool.isEmpty()) DefaultCopies.cat else customPool
            if (pool.isEmpty()) error("猫语池为空")

            // 跳过最近 avoidRecent 条；候选耗尽 => 清空猫语去重池重来（与 randomFor 同语义）。
            val blocked = recents.takeLast(avoidRecent.coerceAtLeast(0)).toSet()
            val candidates = pool.filterNot { it in blocked }
            val chosen: String
            val nextRecents: List<String>
            if (candidates.isEmpty()) {
                chosen = pool.random()
                nextRecents = listOf(chosen)
            } else {
                chosen = candidates.random()
                nextRecents = (recents + chosen).takeLast(RECENT_KEEP_PER_SLOT)
            }

            persistRecents(recentsByGroup, GROUP_CAT, nextRecents)
            return chosen
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

        private suspend fun persistRecents(
            current: Map<String, List<String>>,
            groupName: String,
            next: List<String>,
        ) {
            dataStore.edit { prefs ->
                val updated = current.toMutableMap()
                updated[groupName] = next
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

        /** 存储格式 `["SLOT|句子", …]` ↔ `Map<时段, 最近句列表>`。 */
        private fun entriesFromRecents(recentsBySlot: Map<String, List<String>>): List<String> =
            recentsBySlot.flatMap { (slotName, texts) -> texts.map { "$slotName$ENTRY_SEPARATOR$it" } }

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

            /** 猫语组在去重池持久化格式里的组名（与时段组共用一键，组间互不污染）。 */
            const val GROUP_CAT = "CAT"

            /** 每个时段最多保留的「最近用过的句子」条数（须不小于常用 avoidRecent）。 */
            const val RECENT_KEEP_PER_SLOT = 32
        }
    }
