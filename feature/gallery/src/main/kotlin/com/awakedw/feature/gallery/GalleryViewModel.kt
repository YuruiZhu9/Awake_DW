package com.awakedw.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.Outfit
import com.awakedw.core.model.OutfitCatalog
import com.awakedw.core.model.OutfitCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 一件藏品在画廊里的展示态：[unlocked] 为解锁标记（锁定件呈剪影 + 「第 N 天解锁」期待感文案），
 * [pinned] 为「今日之裙」单选标记（同一时刻至多一件为 true），
 * [isNew] 为未看新解锁标记（卡片左上角「新」标的数据源；进画廊即随未看集清空退场）。
 */
data class GalleryItemUi(
    val outfit: Outfit,
    val unlocked: Boolean,
    val pinned: Boolean,
    val isNew: Boolean = false,
)

/**
 * 画廊页一屏状态（moodboard §5.2）：裙装与馆藏两个分区，各按 [Outfit.unlockDay] 升序。
 */
data class GalleryUiState(
    val dresses: List<GalleryItemUi> = emptyList(),
    val museum: List<GalleryItemUi> = emptyList(),
)

/**
 * 画廊页（衣橱与馆藏）ViewModel。
 *
 * 以 combine(解锁集, 钉选 id, 未看集) 从静态目录 [OutfitCatalog.all] 派生双分区状态：
 * 打卡新解锁落库后回到画廊即时可见，无须重建页面；未看新解锁件带 [GalleryItemUi.isNew]「新」标。
 *
 * 已读语义（用户裁定「无声等待制」）：进画廊即把全部已解锁 id 标记为已看——
 * 未看集清空，首页蝴蝶结圆点与本页「新」标一并退场；先等 [uiState] 首个真实状态就绪再清账，
 * 保证本轮「新」标至少上屏一帧。
 *
 * [pin] 语义（moodboard §5.2「可手动指定今日之裙」）：
 * pin(id) 指定该件；**再点同一件 = 取消钉选（置 null，回到跟随每日随机）**；
 * pin(另一件) = 换指定。当前钉选态以 prefs 的 [UserPreferencesRepository.pinnedOutfitId]
 * 首值为准（权威源），避免界面态与持久层漂移。
 */
@HiltViewModel
class GalleryViewModel
    @Inject
    constructor(
        private val prefs: UserPreferencesRepository,
    ) : ViewModel() {
        val uiState: StateFlow<GalleryUiState> =
            combine(prefs.unlockedOutfits, prefs.pinnedOutfitId, prefs.unseenOutfits) { unlocked, pinned, unseen ->
                GalleryUiState(
                    dresses = partitionOf(OutfitCategory.DRESS, unlocked, pinned, unseen),
                    museum = partitionOf(OutfitCategory.MUSEUM, unlocked, pinned, unseen),
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState())

        init {
            // 进画廊即已读（用户裁定）：等首个真实状态上屏后，把全部已解锁 id 标记为已看。
            viewModelScope.launch {
                uiState.first { it.dresses.isNotEmpty() || it.museum.isNotEmpty() }
                val unlocked = prefs.unlockedOutfits.first()
                if (unlocked.isNotEmpty()) prefs.markOutfitsSeen(unlocked)
            }
        }

        /** 指定「今日之裙」：见类注的三段语义（指定/再点取消/换指定）。 */
        fun pin(outfitId: String?) {
            viewModelScope.launch {
                val current = prefs.pinnedOutfitId.first()
                prefs.setPinnedOutfit(if (outfitId != null && current == outfitId) null else outfitId)
            }
        }

        /** 目录按类别分区 + unlockDay 升序 + 逐件标注解锁/钉选/未看新解锁。 */
        private fun partitionOf(
            category: OutfitCategory,
            unlocked: Set<String>,
            pinned: String?,
            unseen: Set<String>,
        ): List<GalleryItemUi> =
            OutfitCatalog.all
                .filter { it.category == category }
                .sortedBy { it.unlockDay }
                .map { outfit ->
                    GalleryItemUi(
                        outfit = outfit,
                        unlocked = outfit.id in unlocked,
                        pinned = outfit.id == pinned,
                        isNew = outfit.id in unseen,
                    )
                }
    }
