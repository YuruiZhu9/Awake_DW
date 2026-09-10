package com.awakedw.core.data.copy

import com.awakedw.core.model.TimeSlot

/**
 * 内置微文案短句池：打卡确认（[praise]）与猫咪回应（[cat]）专用。
 *
 * 与 [DefaultCopies] 的分工：
 * - [DefaultCopies] 是「心意文案库」的长句，用于首页问候与提醒通知正文，可被使用者整库编辑；
 * - 本池是固定的短句，用在环心确认行与猫语气泡里——这两处空间小、节奏快，
 *   需要的是 3–14 字的即时反应，而不是一句完整的心声。
 *
 * 为什么不让长句兼顾两处：同一句话池同时承担「问候」「打卡回应」「猫的碎碎念」三种语气，
 * 会让打卡瞬间读到一句格言式的自我叙述，也让去重窗口被三处共享而加速撞句。
 *
 * 约束：全部第一人称自述；不出现「你/您/她/他」；不含 `|`（去重池以 `|` 分隔时段与句子）；
 * 每条都能独立成立，不依赖「刚刚喝了水」这一上下文。
 */
object ShortCopies {
    val praiseMorning =
        listOf(
            "记好了，清晨慢慢来",
            "第一杯，稳稳的",
            "嗯，身体醒过来了",
            "这一杯我记下了",
            "很好，就这样开始",
            "清晨的水，刚刚好",
            "记上了，不急不赶",
            "第一杯到位",
            "做得很好，继续",
            "水到了，人也清了",
        )

    val praiseDay =
        listOf(
            "记好了，先松一口气",
            "这一杯，把忙碌隔开",
            "嗯，喉咙舒服了些",
            "记下了，接着忙吧",
            "很好，也歇一下眼睛",
            "午后的水，刚好",
            "记上了，稳稳来",
            "这一杯算数",
            "喝到了，肩膀也松了",
            "中间这一段，补上了",
        )

    val praiseEvening =
        listOf(
            "记好了，今天辛苦了",
            "这一杯，慢慢喝完",
            "嗯，喉咙润了些",
            "记下了，可以歇了",
            "很好，别急着收尾",
            "晚上的水，温温的",
            "记上了，小口就好",
            "这一杯陪我收尾",
            "水到了，心也静了",
            "最后这一杯，安心",
        )

    val catMorning =
        listOf(
            "喵~",
            "呼噜呼噜",
            "我醒得刚刚好",
            "再待一会儿也行",
            "我就在这里陪着",
            "尾巴轻轻动了动",
            "早上的空气是软的",
            "嗯，现在很舒服",
            "喵呜，慢慢来",
            "太阳刚好照到手边",
        )

    val catDay =
        listOf(
            "眯一会儿也可以",
            "伸个懒腰，喵",
            "窗边的光有点亮",
            "爪子收起来了",
            "嗯，午后是暖的",
            "喵呜，正好",
            "靠一会儿就够",
            "尾巴垂在桌沿",
            "有点想打盹",
            "我趴着不动",
        )

    val catEvening =
        listOf(
            "夜里安静下来了",
            "困了，喵",
            "灯影晃了一下",
            "尾巴搭在脚边",
            "嗯，可以休息了",
            "喵呜，晚安",
            "眼睛快闭上了",
            "缩成一小团",
            "今晚不出声",
            "打个小呼噜",
        )

    /** 打卡确认短句池。 */
    fun praiseOf(slot: TimeSlot): List<String> =
        when (slot) {
            TimeSlot.MORNING -> praiseMorning
            TimeSlot.DAY -> praiseDay
            TimeSlot.EVENING -> praiseEvening
        }

    /** 猫咪回应短句池。 */
    fun catOf(slot: TimeSlot): List<String> =
        when (slot) {
            TimeSlot.MORNING -> catMorning
            TimeSlot.DAY -> catDay
            TimeSlot.EVENING -> catEvening
        }
}
