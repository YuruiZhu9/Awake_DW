package com.awakedw.core.data.copy

import com.awakedw.core.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 内置语料池守则：打卡引文与猫咪回应分属两个池、两套写法纪律。
 *
 * 这些约束不是审美偏好，而是**排版与存储的硬前提**：
 * - 长度上限保证环心两行放得下、气泡内缩成小胶囊；
 * - 不含 `|` 保证去重池 `"<池键>|句子"` 能正确解析；
 * - 不出现「你/您」保证全应用不对使用者说话——
 *   第三人称是允许的：引文叙述与作者落款都会用到（视觉基线 §12.1）；
 * - 两个池不重叠保证同屏时（环心引文 + 猫语气泡）不会读到同一句话。
 */
class ShortCopiesTest {
    private val praisePools =
        mapOf(
            "打卡·早" to ShortCopies.praiseMorning,
            "打卡·昼" to ShortCopies.praiseDay,
            "打卡·晚" to ShortCopies.praiseEvening,
        )

    private val catPools =
        mapOf(
            "猫语·早" to ShortCopies.catMorning,
            "猫语·昼" to ShortCopies.catDay,
            "猫语·晚" to ShortCopies.catEvening,
        )

    @Test
    fun `打卡引文每时段十条且正文全局唯一`() {
        praisePools.forEach { (name, pool) ->
            assertEquals("$name 应为 10 条", 10, pool.size)
            assertEquals("$name 组内正文不得重复", pool.size, pool.map { it.text }.toSet().size)
        }
        val allTexts = praisePools.values.flatten().map { it.text }
        assertEquals("30 条打卡引文的正文必须两两不同", 30, allTexts.toSet().size)
    }

    @Test
    fun `打卡引文正文长度落在可排版区间且不含去重池分隔符`() {
        praisePools.forEach { (name, pool) ->
            pool.forEach { quote ->
                assertTrue("$name「${quote.text}」不应为空", quote.text.isNotBlank())
                assertTrue(
                    "$name「${quote.text}」应落在 2–15 字（15 是七言联句里那个全角逗号的位置）",
                    quote.text.length in 2..15,
                )
                assertFalse("$name「${quote.text}」不得含 |", quote.text.contains('|'))
            }
        }
    }

    @Test
    fun `打卡引文不出现第二人称`() {
        val forbidden = listOf('你', '您')
        praisePools.forEach { (name, pool) ->
            pool.forEach { quote ->
                forbidden.forEach { ch ->
                    assertFalse("$name「${quote.text}」出现「$ch」，破坏了「不对使用者说话」", quote.text.contains(ch))
                }
            }
        }
    }

    /**
     * 落款的形态守则：要么是一位作者，要么干脆不写。
     *
     * 测试守不住「出处是否真实」——那靠写作时逐条核对；但守得住**不许拿占位符凑数**：
     * 空串、超长、带分隔符都会让「小票引文」这一形态崩掉，而「佚名」这类写法
     * 本质上是在为一句原创句伪造出处。
     */
    @Test
    fun `落款要么是作者名要么干脆不写`() {
        val attributed = praisePools.values.flatten().mapNotNull { it.attribution }
        assertTrue("应有带落款的引文", attributed.isNotEmpty())
        attributed.forEach { name ->
            assertTrue("落款不得为空串", name.isNotBlank())
            assertTrue("落款「$name」应短于 7 字", name.length in 2..6)
            assertFalse("落款「$name」不得含 |", name.contains('|'))
        }
        praisePools.forEach { (name, pool) ->
            assertTrue("$name 应有带落款的引文", pool.any { it.attribution != null })
            assertTrue(
                "$name 应有不署落款的原创句——用户明确要求不要整池都是文段",
                pool.any { it.attribution == null },
            )
        }
    }

    @Test
    fun `猫语池每时段十条且守则不变`() {
        val forbidden = listOf('你', '您', '她', '他')
        catPools.forEach { (name, pool) ->
            assertEquals("$name 应为 10 条", 10, pool.size)
            assertEquals("$name 组内不得重复", pool.size, pool.toSet().size)
            pool.forEach { line ->
                assertTrue("$name「$line」不应为空", line.isNotBlank())
                assertTrue("$name「$line」应短于 15 字，气泡才放得下", line.length in 2..14)
                assertFalse("$name「$line」不得含 |", line.contains('|'))
                forbidden.forEach { ch ->
                    assertFalse("$name「$line」出现「$ch」，破坏猫的自述口吻", line.contains(ch))
                }
            }
        }
    }

    @Test
    fun `打卡引文与猫咪回应不共享任何一句`() {
        val praise = praisePools.values.flatten().map { it.text }
        val cat = catPools.values.flatten()
        assertTrue("同屏出现的两个池不得重叠", praise.intersect(cat.toSet()).isEmpty())
        assertEquals("六组合计 60 条且全局唯一", 60, (praise + cat).toSet().size)
    }

    @Test
    fun `按时段取池与分组常量一致`() {
        listOf(TimeSlot.MORNING, TimeSlot.DAY, TimeSlot.EVENING).forEach { slot ->
            assertEquals(10, ShortCopies.praiseOf(slot).size)
            assertEquals(10, ShortCopies.catOf(slot).size)
        }
        assertEquals(ShortCopies.praiseMorning, ShortCopies.praiseOf(TimeSlot.MORNING))
    }
}
