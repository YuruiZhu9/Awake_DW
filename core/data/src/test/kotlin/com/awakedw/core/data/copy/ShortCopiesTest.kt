package com.awakedw.core.data.copy

import com.awakedw.core.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 内置短句池守则：打卡确认与猫咪回应共用一套写法纪律，但分属两个池。
 *
 * 这些约束不是审美偏好，而是**排版与存储的硬前提**：
 * - 长度上限保证环心一行放得下、气泡内缩成小胶囊；
 * - 不含 `|` 保证去重池 `"<池键>|句子"` 能正确解析；
 * - 不出现第二/第三人称保证全应用的自述口吻一致；
 * - 两个池不重叠保证同屏时（环心 + 气泡）不会读到同一句话。
 */
class ShortCopiesTest {
    private val pools =
        mapOf(
            "打卡·早" to ShortCopies.praiseMorning,
            "打卡·昼" to ShortCopies.praiseDay,
            "打卡·晚" to ShortCopies.praiseEvening,
            "猫语·早" to ShortCopies.catMorning,
            "猫语·昼" to ShortCopies.catDay,
            "猫语·晚" to ShortCopies.catEvening,
        )

    @Test
    fun `短句池每时段十条且组内无重复`() {
        pools.forEach { (name, pool) ->
            assertEquals("$name 应为 10 条", 10, pool.size)
            assertEquals("$name 组内不得重复", pool.size, pool.toSet().size)
        }
    }

    @Test
    fun `短句长度落在可排版区间且不含去重池分隔符`() {
        pools.forEach { (name, pool) ->
            pool.forEach { line ->
                assertTrue("$name「$line」不应为空", line.isNotBlank())
                assertTrue("$name「$line」应短于 15 字，环心与胶囊才放得下", line.length in 2..14)
                assertFalse("$name「$line」不得含 |", line.contains('|'))
            }
        }
    }

    @Test
    fun `短句不出现第二或第三人称`() {
        val forbidden = listOf('你', '您', '她', '他')
        pools.forEach { (name, pool) ->
            pool.forEach { line ->
                forbidden.forEach { ch ->
                    assertFalse("$name「$line」出现「$ch」，破坏自述口吻", line.contains(ch))
                }
            }
        }
    }

    @Test
    fun `打卡确认与猫咪回应不共享任何一句`() {
        val praise = ShortCopies.praiseMorning + ShortCopies.praiseDay + ShortCopies.praiseEvening
        val cat = ShortCopies.catMorning + ShortCopies.catDay + ShortCopies.catEvening
        assertTrue("同屏出现的两个池不得重叠", praise.intersect(cat.toSet()).isEmpty())
        assertEquals("六组合计 60 条且全局唯一", 60, (praise + cat).toSet().size)
    }

    @Test
    fun `按时段取池与分组常量一致`() {
        listOf(TimeSlot.MORNING, TimeSlot.DAY, TimeSlot.EVENING).forEach { slot ->
            assertEquals(10, ShortCopies.praiseOf(slot).size)
            assertEquals(10, ShortCopies.catOf(slot).size)
        }
    }
}
