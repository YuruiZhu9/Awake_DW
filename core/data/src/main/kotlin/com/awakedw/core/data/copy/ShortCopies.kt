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
 *   中国古典诗文摘句（带作者落款）与原创短句（不带落款）混排，读起来像随手翻到的一句闲话，
 *   而不是一句自我评价；
 * - `cat*` 是**猫自己的碎念**，仍是 3–14 字的自述短句，与引文池互不重叠。
 *
 * 约束（由 `ShortCopiesTest` 守着）：
 * - 正文 2–15 字（15 是给七言联句中间那个全角逗号留的位置）；
 * - 不含 `|`（去重池以 `|` 分隔池键与句子）；
 * - **不出现第二人称「你/您」**——全应用不对使用者说话；
 *   第三人称不再禁止：引文叙述与作者名都会用到（视觉基线 §12.1）；
 * - 全局文本唯一；原创句一律不署落款，绝不编造出处。
 */
object ShortCopies {
    val praiseMorning =
        listOf(
            PraiseQuote("清晨入古寺，初日照高林", "常建"),
            PraiseQuote("晓雾将歇，猿鸟乱鸣", "陶弘景"),
            PraiseQuote("小楼一夜听春雨，深巷明朝卖杏花", "陆游"),
            PraiseQuote("鸡声茅店月，人迹板桥霜", "温庭筠"),
            PraiseQuote("木欣欣以向荣，泉涓涓而始流", "陶渊明"),
            PraiseQuote("日出江花红胜火，春来江水绿如蓝", "白居易"),
            PraiseQuote("新晴原野旷，极目无氛垢", "王维"),
            PraiseQuote("晨光落在水面上，晃了晃"),
            PraiseQuote("杯子是凉的，手心慢慢暖起来"),
            PraiseQuote("窗子刚亮，水已经倒好了"),
        )

    val praiseDay =
        listOf(
            PraiseQuote("树阴满地日当午，梦觉流莺时一声", "苏舜钦"),
            PraiseQuote("荷风送香气，竹露滴清响", "孟浩然"),
            PraiseQuote("纸屏石枕竹方床，手倦抛书午梦长", "蔡确"),
            PraiseQuote("竹深树密虫鸣处，时有微凉不是风", "杨万里"),
            PraiseQuote("水光潋滟晴方好，山色空蒙雨亦奇", "苏轼"),
            PraiseQuote("野芳发而幽香，佳木秀而繁阴", "欧阳修"),
            PraiseQuote("枕上诗书闲处好，门前风景雨来佳", "李清照"),
            PraiseQuote("水放在手边，一下午就慢了"),
            PraiseQuote("光挪了半寸，杯沿还留着凉"),
            PraiseQuote("停在这里，听见水咽下去的声音"),
        )

    val praiseEvening =
        listOf(
            PraiseQuote("晚来天欲雪，能饮一杯无", "白居易"),
            PraiseQuote("何当共剪西窗烛，却话巴山夜雨时", "李商隐"),
            PraiseQuote("欲持一瓢酒，远慰风雨夕", "韦应物"),
            PraiseQuote("渡头余落日，墟里上孤烟", "王维"),
            PraiseQuote("落霞与孤鹜齐飞，秋水共长天一色", "王勃"),
            PraiseQuote("更深月色半人家，北斗阑干南斗斜", "刘方平"),
            PraiseQuote("月落乌啼霜满天，江枫渔火对愁眠", "张继"),
            PraiseQuote("灯下这一杯，喝得比白天慢"),
            PraiseQuote("杯子搁在灯影里，夜就静了"),
            PraiseQuote("最后这一口，不用急着喝完"),
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
