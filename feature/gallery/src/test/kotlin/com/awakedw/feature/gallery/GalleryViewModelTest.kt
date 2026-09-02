package com.awakedw.feature.gallery

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.OutfitCatalog
import com.awakedw.core.model.OutfitCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 画廊页 ViewModel（moodboard §5.2 收集循环，纯 JVM + Turbine）：
 * - init 即以 combine(unlockedOutfits, pinnedOutfitId) 派生双分区状态：
 *   裙装/馆藏各按 unlockDay 升序，逐件标注 unlocked 与 pinned；
 * - pin(id)：指定「今日之裙」；再点同一件 = 取消钉选（置 null）；切到另一件 = 换指定；
 * - 解锁集后续变化实时回流到状态（打卡解锁回到画廊立即可见）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：假 prefs + UnconfinedTestDispatcher 主调度器，状态沉降后断言。 */
    private fun harness(
        scheduler: TestCoroutineScheduler,
        prefs: FakeGalleryPrefs,
    ): GalleryViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        return GalleryViewModel(prefs)
    }

    /** 双分区合计至多一件钉选（单选语义）：返回被钉件的 id，无钉选返回 null。 */
    private fun GalleryUiState.pinnedIdOrNull(): String? = (dresses + museum).singleOrNull { it.pinned }?.outfit?.id

    /** 等到「被钉件 = [pinnedId]（null = 全部取消）」的状态；跳过种子值等过渡帧。 */
    private suspend fun TurbineTestContext<GalleryUiState>.awaitPinned(pinnedId: String?): GalleryUiState {
        while (true) {
            val state = awaitItem()
            if (state.pinnedIdOrNull() == pinnedId) return state
        }
    }

    @Test
    fun `初始状态_双分区按unlockDay升序_未解锁件unlocked为false`() =
        runTest {
            val prefs = FakeGalleryPrefs()
            prefs.markOutfitsUnlocked(setOf("dress_00", "dress_01", "dress_02", "museum_01"))
            val viewModel = harness(testScheduler, prefs)
            runCurrent()

            val state = viewModel.uiState.value

            // 目录顺序是交错的，双分区必须各自按 unlockDay 升序重排。
            assertEquals(
                OutfitCatalog.all.filter { it.category == OutfitCategory.DRESS }.sortedBy { it.unlockDay }.map { it.id },
                state.dresses.map { it.outfit.id },
            )
            assertEquals(
                OutfitCatalog.all.filter { it.category == OutfitCategory.MUSEUM }.sortedBy { it.unlockDay }.map { it.id },
                state.museum.map { it.outfit.id },
            )

            // 解锁标记逐件对齐：解锁集内 true，其余（含全部锁定件）false。
            state.dresses.forEach { item ->
                assertEquals("dress: ${item.outfit.id}", item.outfit.id in prefs.unlockedOutfits.first(), item.unlocked)
            }
            state.museum.forEach { item ->
                assertEquals("museum: ${item.outfit.id}", item.outfit.id in prefs.unlockedOutfits.first(), item.unlocked)
            }
            assertTrue(state.dresses.first { it.outfit.id == "dress_00" }.unlocked)
            assertFalse(state.dresses.first { it.outfit.id == "dress_07" }.unlocked)
            // 无钉选时全部件 pinned=false。
            assertTrue(state.dresses.none { it.pinned } && state.museum.none { it.pinned })
        }

    @Test
    fun `pin指定今日之裙_该件pinned为true其余为false`() =
        runTest {
            val prefs = FakeGalleryPrefs()
            prefs.markOutfitsUnlocked(setOf("dress_00", "dress_01", "dress_02", "museum_01"))
            val viewModel = harness(testScheduler, prefs)
            runCurrent()

            viewModel.uiState.test {
                viewModel.pin("dress_01")

                val state = awaitPinned("dress_01")
                assertTrue(state.dresses.first { it.outfit.id == "dress_01" }.pinned)
                // 其余裙装与全部馆藏一律 false（pinned 是单选）。
                assertTrue(state.dresses.filter { it.outfit.id != "dress_01" }.none { it.pinned })
                assertTrue(state.museum.none { it.pinned })
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals("dress_01", prefs.pinnedOutfitId.first())
        }

    @Test
    fun `再pin同一件则取消钉选置null`() =
        runTest {
            val prefs = FakeGalleryPrefs()
            prefs.markOutfitsUnlocked(setOf("dress_00", "dress_01", "dress_02", "museum_01"))
            val viewModel = harness(testScheduler, prefs)
            runCurrent()

            viewModel.uiState.test {
                viewModel.pin("dress_01")
                awaitPinned("dress_01")

                // 再点同一件 = 取消指定：回 null，回到「跟随每日随机」。
                viewModel.pin("dress_01")
                val cleared = awaitPinned(null)
                assertTrue(cleared.dresses.none { it.pinned } && cleared.museum.none { it.pinned })
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(null, prefs.pinnedOutfitId.first())
        }

    @Test
    fun `pin切到另一件_旧件取消新件钉选`() =
        runTest {
            val prefs = FakeGalleryPrefs()
            prefs.markOutfitsUnlocked(setOf("dress_00", "dress_01", "dress_02", "museum_01"))
            val viewModel = harness(testScheduler, prefs)
            runCurrent()

            viewModel.uiState.test {
                viewModel.pin("dress_01")
                awaitPinned("dress_01")

                viewModel.pin("museum_01")
                val state = awaitPinned("museum_01")
                assertTrue(state.dresses.none { it.pinned })
                assertTrue(state.museum.first { it.outfit.id == "museum_01" }.pinned)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals("museum_01", prefs.pinnedOutfitId.first())
        }

    @Test
    fun `解锁集后续变化实时回流到状态`() =
        runTest {
            val prefs = FakeGalleryPrefs()
            prefs.markOutfitsUnlocked(setOf("dress_00"))
            val viewModel = harness(testScheduler, prefs)
            runCurrent()
            assertFalse(viewModel.uiState.value.dresses.first { it.outfit.id == "dress_01" }.unlocked)

            // 打卡解锁落库（幂等合并）后，画廊状态无须重建即翻转 unlocked。
            viewModel.uiState.test {
                prefs.markOutfitsUnlocked(setOf("dress_01", "museum_01"))
                var state = awaitItem()
                while (!state.dresses.first { it.outfit.id == "dress_01" }.unlocked) {
                    state = awaitItem()
                }
                assertTrue(state.dresses.first { it.outfit.id == "dress_01" }.unlocked)
                assertTrue(state.museum.first { it.outfit.id == "museum_01" }.unlocked)
                assertFalse(state.dresses.first { it.outfit.id == "dress_07" }.unlocked)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `未看新解锁件isNew为true_init标记已看后随未看集清空退场`() =
        runTest {
            val prefs = FakeGalleryPrefs()
            prefs.markOutfitsUnlocked(setOf("dress_00", "dress_01", "dress_02"))
            prefs.markOutfitsUnseen(setOf("dress_01", "dress_02"))
            // 门闩假件拦住 markOutfitsSeen：制造「首帧状态已就绪、清账未落」的过渡，供断言「新」标确实亮过。
            val gate = CompletableDeferred<Unit>()
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = GalleryViewModel(GatedGalleryPrefs(prefs, gate))
            runCurrent()

            // 进画廊第一帧：未看件 isNew=true；既有件与锁定件一律 false。
            val first = viewModel.uiState.value
            assertTrue(first.dresses.first { it.outfit.id == "dress_01" }.isNew)
            assertTrue(first.dresses.first { it.outfit.id == "dress_02" }.isNew)
            assertFalse(first.dresses.first { it.outfit.id == "dress_00" }.isNew)
            assertTrue((first.dresses + first.museum).none { it.isNew && !it.unlocked })

            // 放行清账（init 已把全部已解锁 id 标记为已看）：未看集清空、isNew 全 false。
            gate.complete(Unit)
            runCurrent()
            assertEquals(emptySet<String>(), prefs.unseenOutfits.first())
            assertTrue((viewModel.uiState.value.dresses + viewModel.uiState.value.museum).none { it.isNew })
        }

    /** 门闩假件：其余成员按接口委托直通 [delegate]，仅 markOutfitsSeen 等待 [gate] 放行。 */
    private class GatedGalleryPrefs(
        private val delegate: UserPreferencesRepository,
        private val gate: CompletableDeferred<Unit>,
    ) : UserPreferencesRepository by delegate {
        override suspend fun markOutfitsSeen(ids: Collection<String>) {
            gate.await()
            delegate.markOutfitsSeen(ids)
        }
    }
}
