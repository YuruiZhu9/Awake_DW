package com.awakedw.core.designsystem

import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId

/** 主题选项的中文名（2.4.0 自 settings 提升共享）：引导步与设置页用同一份叫法。 */
fun themeLabelOf(choice: ThemeChoice): String =
    when (choice) {
        ThemeChoice.FOLLOW_TIME -> "随时间"
        ThemeChoice.FIXED_EMERALD -> "晨雾蓝瓷"
        ThemeChoice.FIXED_STRAWBERRY -> "午后藕荷"
        ThemeChoice.FIXED_CARAMEL -> "黄昏奶茶"
        ThemeChoice.FIXED_NIGHT -> "深夜青黛"
        ThemeChoice.FIXED_LAVENDER -> "雾紫玫瑰"
        ThemeChoice.FIXED_GOTHIC -> "黑色哥特"
        ThemeChoice.FIXED_CLERIC -> "白色圣职"
        ThemeChoice.FIXED_THIN_MINT -> "薄荷巧克力"
    }

/** 主题选项到具体主题的映射：「随时间」无固定主题，落 EMERALD 仅供缩略图兜底。 */
fun themeIdOf(choice: ThemeChoice): ThemeId =
    when (choice) {
        ThemeChoice.FIXED_EMERALD -> ThemeId.EMERALD
        ThemeChoice.FIXED_STRAWBERRY -> ThemeId.STRAWBERRY
        ThemeChoice.FIXED_CARAMEL -> ThemeId.CARAMEL
        ThemeChoice.FIXED_NIGHT -> ThemeId.NIGHT
        ThemeChoice.FIXED_LAVENDER -> ThemeId.LAVENDER
        ThemeChoice.FIXED_GOTHIC -> ThemeId.GOTHIC
        ThemeChoice.FIXED_CLERIC -> ThemeId.CLERIC
        ThemeChoice.FIXED_THIN_MINT -> ThemeId.THIN_MINT
        ThemeChoice.FOLLOW_TIME -> ThemeId.EMERALD
    }
