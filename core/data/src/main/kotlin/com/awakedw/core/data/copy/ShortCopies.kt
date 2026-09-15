package com.awakedw.core.data.copy

import com.awakedw.core.model.PraiseQuote
import com.awakedw.core.model.TimeSlot

/**
 * 内置微文案池：打卡确认（[praiseMorning] 等）与猫咪回应（[catMorning] 等）专用。
 *
 * 与 [DefaultCopies] 的分工：
 * - [DefaultCopies] 是「心意文案库」的长句，用于首页问候与提醒通知正文，可被使用者整库编辑；
 * - 本池是固定的句子，用在环心确认行与猫语气泡里——这两处空间小、节奏快。
 *
 * 两个池的语气分工（视觉基线 §12）：
 * - `praise*` 是**打卡那一刻给使用者看的一句话**，形态取自取餐小票上的引文：
 *   现代白话语录（带「作者《作品名》」落款）与原创短句（不带落款）混排，
 *   读起来像随手翻到的一句闲话，而不是一句自我评价；
 *   语料只取现代中文散文摘句（公有领域、原文逐字）与西方文学短句（原文公有领域、本项目自译），
 *   **不含任何文言**（2026-09-15 使用者确认：文言骈句的读感仍是诗，全部不用）；
 * - `cat*` 是**猫自己的碎念**，仍是 3–14 字的自述短句，与引文池互不重叠。
 *
 * 约束（由 `ShortCopiesTest` 守着）：
 * - 正文 2–15 字，一眼读完；
 * - 不含 `|`（去重池以 `|` 分隔池键与句子）；
 * - **不出现第二人称「你/您」**——全应用不对使用者说话；
 *   第三人称不再禁止：引文叙述与作者名都会用到（视觉基线 §12.1）；
 * - 全局文本唯一；原创句一律不署落款，绝不编造出处。
 */
object ShortCopies {
    val praiseMorning =
        listOf(
            PraiseQuote("醒着，便是活着", "梭罗《瓦尔登湖》"),
            PraiseQuote("每个清晨，都是一张愉快的请柬", "梭罗《瓦尔登湖》"),
            PraiseQuote("明天总是崭新的，还没有任何过错", "蒙哥马利《绿山墙的安妮》"),
            PraiseQuote("燕子去了，有再来的时候", "朱自清《匆匆》"),
            PraiseQuote("杨柳枯了，有再青的时候", "朱自清《匆匆》"),
            PraiseQuote("比起恭维，我现在更想要咖啡", "阿尔柯特《小妇人》"),
            PraiseQuote("晨光落在水面上，晃了晃"),
            PraiseQuote("杯子是凉的，手心慢慢暖起来"),
            PraiseQuote("窗子刚亮，水已经倒好了"),
            PraiseQuote("先喝一口，再把今天打开"),
        )

    val praiseDay =
        listOf(
            PraiseQuote("得半日之闲，可抵十年的尘梦", "周作人《喝茶》"),
            PraiseQuote("喝茶之后，去继续修各人的胜业", "周作人《喝茶》"),
            PraiseQuote("比海更壮观的，是天空", "雨果《悲惨世界》"),
            PraiseQuote("对心中未解之事，要有耐心", "里尔克《给青年诗人的信》"),
            PraiseQuote("时间是我垂钓其中的溪流", "梭罗《瓦尔登湖》"),
            PraiseQuote("本质的东西，用眼睛是看不见的", "圣埃克苏佩里《小王子》"),
            PraiseQuote("爱自己，是终身浪漫的开始", "王尔德《理想丈夫》"),
            PraiseQuote("水放在手边，一下午就慢了"),
            PraiseQuote("光挪了半寸，杯沿还留着凉"),
            PraiseQuote("停在这里，听见水咽下去的声音"),
        )

    val praiseEvening =
        listOf(
            PraiseQuote("都在沟渠里，仍有人望着星星", "王尔德《温德米尔夫人的扇子》"),
            PraiseQuote("月光如流水一般", "朱自清《荷塘月色》"),
            PraiseQuote("我的日子滴在时间的流里", "朱自清《匆匆》"),
            PraiseQuote("夜间睡在舱中，听水声橹声", "周作人《乌篷船》"),
            PraiseQuote("太阳不过是一颗晨星", "梭罗《瓦尔登湖》"),
            PraiseQuote("灯下这一杯，喝得比白天慢"),
            PraiseQuote("杯子搁在灯影里，夜就静了"),
            PraiseQuote("最后这一口，不用急着喝完"),
            PraiseQuote("夜里的水，是白天的句号"),
            PraiseQuote("把杯子洗好，今天就算收好了"),
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

    /** 打卡确认池。 */
    fun praiseOf(slot: TimeSlot): List<PraiseQuote> =
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
