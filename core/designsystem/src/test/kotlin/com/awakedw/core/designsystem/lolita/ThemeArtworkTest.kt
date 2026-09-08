package com.awakedw.core.designsystem.lolita

import com.awakedw.core.designsystem.art.loadAssetBitmap
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeArtworkTest {
    @Test
    fun `every theme has exactly one frame source without doubling existing artwork`() {
        val pictureFrames = setOf(ThemeId.GOTHIC, ThemeId.CLERIC, ThemeId.THIN_MINT)
        ThemeId.entries.forEach { id ->
            assertEquals(id !in pictureFrames, usesDrawnLaceFrame(id))
            assertTrue(usesDrawnLaceFrame(id) xor themeArtworkOf(id).framed)
        }
    }

    @Test
    fun `detailed backgrounds use legible content surfaces without changing old themes`() {
        assertEquals(0.94f, artworkPanelOpacity(ThemeId.THIN_MINT, 0.26f))
        assertEquals(0.94f, artworkPanelOpacity(ThemeId.GOTHIC, 0.64f))
        assertEquals(0.94f, artworkPanelOpacity(ThemeId.CLERIC, 0.58f))
        assertEquals(0.26f, artworkPanelOpacity(ThemeId.EMERALD, 0.26f))
    }

    @Test
    fun `dedicated artwork files decode and stay within the packaged size budget`() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication()
            listOf(ThemeId.THIN_MINT, ThemeId.GOTHIC, ThemeId.CLERIC).forEach { id ->
                val art = themeArtworkOf(id)
                assertTrue(art.framed)
                assertTrue(art.opacity in 0.5f..1f)
                assertTrue(context.assets.open(art.asset).use { it.readBytes().size } < 350_000)
                val bitmap = loadAssetBitmap(context, art.asset)
                assertNotNull(bitmap)
                assertEquals(1440, bitmap!!.height)
            }
        }

    @Test
    fun `gothic painted assets are never inverted and cleric is no longer a fallback`() {
        assertEquals(ArtworkTreatment.PAINTED, themeArtworkOf(ThemeId.GOTHIC).treatment)
        assertEquals(ArtworkTreatment.PAINTED, themeArtworkOf(ThemeId.CLERIC).treatment)
        assertEquals("lolita/cleric.jpg", themeArtworkOf(ThemeId.CLERIC).asset)
    }
}
