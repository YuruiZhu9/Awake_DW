package com.awakedw.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitCatalogTest {
    @Test
    fun `目录约束 - id唯一且note不超长`() {
        val ids = OutfitCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        OutfitCatalog.all.forEach { assertTrue("${it.id} note过长", it.note.length <= 50) }
    }

    @Test
    fun `解锁曲线 - 开局一件百天全量`() {
        assertEquals(1, OutfitCatalog.unlockedBy(0).size)
        assertEquals(OutfitCatalog.all.size, OutfitCatalog.unlockedBy(100).size)
        assertTrue(OutfitCatalog.unlockedBy(0).all { it.unlockDay == 0 })
        assertEquals(OutfitCatalog.all.filter { it.unlockDay <= 100 }.sortedBy { it.unlockDay }, OutfitCatalog.unlockedBy(100))
    }

    @Test
    fun `目录约束 - id非空且unlockDay非负且DRESS严格升序`() {
        OutfitCatalog.all.forEach {
            assertTrue("${it.id} id为空", it.id.isNotBlank())
            assertTrue("${it.id} unlockDay为负", it.unlockDay >= 0)
        }
        val dresses = OutfitCatalog.all.filter { it.category == OutfitCategory.DRESS }
        dresses.zipWithNext().forEach { (a, b) ->
            assertTrue("DRESS unlockDay非严格升序: ${a.id} -> ${b.id}", a.unlockDay < b.unlockDay)
        }
    }

    @Test
    fun `byId 命中与未命中`() {
        assertEquals(OutfitCategory.DRESS, OutfitCatalog.byId("dress_00")?.category)
        assertNull(OutfitCatalog.byId("nope"))
    }
}
