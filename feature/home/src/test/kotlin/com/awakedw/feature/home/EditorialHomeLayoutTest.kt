package com.awakedw.feature.home

import com.awakedw.core.designsystem.components.editorialDateLabel
import com.awakedw.core.designsystem.components.editorialFolio
import com.awakedw.core.designsystem.components.editorialPageLine
import com.awakedw.core.designsystem.lolita.ArtworkCrop
import com.awakedw.core.designsystem.lolita.artworkSourceRect
import com.awakedw.feature.home.components.editorialGoalLine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class EditorialHomeLayoutTest {
    @Test
    fun `editorial masthead formats the date as a quiet page heading`() {
        assertEquals("9月24日 · 星期四", editorialDateLabel(LocalDateTime.of(2026, 9, 24, 9, 0)))
    }

    @Test
    fun `folio is the day-of-month padded to two digits`() {
        assertEquals("04", editorialFolio(LocalDateTime.of(2026, 9, 4, 9, 0)))
        assertEquals("17", editorialFolio(LocalDateTime.of(2026, 11, 17, 23, 40)))
    }

    @Test
    fun `page line keeps the printed-page reading`() {
        assertEquals("第 04 页", editorialPageLine("04"))
    }

    @Test
    fun `editorial goal line stays factual before and after the goal`() {
        assertEquals("每日目标 1600ml · 还差 350ml", editorialGoalLine(totalMl = 1250, goalMl = 1600))
        assertEquals("每日目标 1600ml · 已达成", editorialGoalLine(totalMl = 1600, goalMl = 1600))
    }

    @Test
    fun `artwork crop stays inside the image at every corner`() {
        // 832×1472 的立绘：zoom=2 → 取 416px 正方形。
        val topLeft = artworkSourceRect(832, 1472, ArtworkCrop(focusX = 0f, focusY = 0f, zoom = 2f))
        assertEquals(0f, topLeft.left, 0.001f)
        assertEquals(0f, topLeft.top, 0.001f)
        assertEquals(416f, topLeft.width, 0.001f)

        val bottomRight = artworkSourceRect(832, 1472, ArtworkCrop(focusX = 1f, focusY = 1f, zoom = 2f))
        assertEquals(416f, bottomRight.left, 0.001f)
        assertEquals(1056f, bottomRight.top, 0.001f)
        assertEquals(416f, bottomRight.width, 0.001f)

        // zoom=1 时取短边整幅，焦点再偏也不越界。
        val wide = artworkSourceRect(832, 1472, ArtworkCrop(focusX = 0.9f, focusY = 0.9f, zoom = 1f))
        assertEquals(0f, wide.left, 0.001f)
        assertEquals(832f, wide.width, 0.001f)
        assertEquals(1472f, wide.bottom, 0.001f)
    }

    @Test
    fun `wide artwork crop keeps the plate aspect and stays inside the image`() {
        // 统计卷首的横裁：目标约 3:1，任何倍率下源矩形都保持该宽高比。
        val rect = artworkSourceRect(832, 1472, ArtworkCrop(focusX = 0.5f, focusY = 0.42f, zoom = 1.55f), aspectRatio = 3f)
        assertEquals(3f, rect.width / rect.height, 0.01f)
        assertEquals(true, rect.left >= 0f && rect.top >= 0f)
        assertEquals(true, rect.right <= 832f && rect.bottom <= 1472f)

        // 焦点压到角上也不越界，且宽高比不因夹紧而破坏。
        val corner = artworkSourceRect(832, 1472, ArtworkCrop(focusX = 1f, focusY = 1f, zoom = 1.2f), aspectRatio = 3f)
        assertEquals(3f, corner.width / corner.height, 0.01f)
        assertEquals(832f, corner.right, 0.001f)
        assertEquals(1472f, corner.bottom, 0.001f)
    }
}
