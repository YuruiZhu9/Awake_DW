package com.awakedw.core.model

/** 裙装目录类别：AI 产线的洛丽塔裙 / 博物馆公有领域名画（moodboard §5.2 馆藏分区）。 */
enum class OutfitCategory { DRESS, MUSEUM }

/**
 * 一件「今日穿搭」藏品。[assetFile] 为 :app assets 相对路径；
 * [unlockDay] 为连续达标天数门槛，0 = 开局即有。
 * 夜变体约定存在时与主图同目录、文件名去扩展名加 `_night.webp`。
 */
data class Outfit(
    val id: String,
    val title: String,
    val note: String,
    val category: OutfitCategory,
    val assetFile: String,
    val unlockDay: Int,
)

/** 静态目录：v0.2 首发 12 件（8 裙 + 4 馆藏）。改动须过 [OutfitCatalogTest] 的目录约束。 */
object OutfitCatalog {
    val all: List<Outfit> =
        listOf(
            Outfit("dress_00", "素呢初见", "本衣柜的第一件：未染的素色软纱，像还没讲出口的早晨。", OutfitCategory.DRESS, "outfit/dress_00.webp", 0),
            Outfit("dress_01", "天水碧·茶会", "天水碧染的轻纱裙摆，茶会还没开始，猫已经先坐好了。", OutfitCategory.DRESS, "outfit/dress_01.webp", 3),
            Outfit("dress_02", "十样锦·午后", "十样锦的甜，是从裙撑边缘一点点漫出来的。", OutfitCategory.DRESS, "outfit/dress_02.webp", 7),
            Outfit("museum_01", "读书少女", "弗拉戈纳尔笔下的少女，读书读累了也会先喝口水吧。", OutfitCategory.MUSEUM, "outfit/museum_01.webp", 5),
            Outfit("museum_02", "猫的肖像", "隆纳-克尼普画笔下的猫，据说是本大王的远房姑妈。", OutfitCategory.MUSEUM, "outfit/museum_02.webp", 10),
            Outfit("dress_03", "藕荷·雨歇", "雨刚停，藕荷色的裙褶里还藏着一点云。", OutfitCategory.DRESS, "outfit/dress_03.webp", 14),
            Outfit("dress_04", "缃叶·黄昏", "缃叶色的裙摆扫过黄昏，把落日别在了腰后。", OutfitCategory.DRESS, "outfit/dress_04.webp", 21),
            Outfit("museum_03", "水彩天鹅", "萨金特笔下的天鹅，水面是一整杯温柔的光。", OutfitCategory.MUSEUM, "outfit/museum_03.webp", 25),
            Outfit("dress_05", "月白·夜曲", "月白色的夜裙，缀着猫眼石，走路有月光的声音。", OutfitCategory.DRESS, "outfit/dress_05.webp", 30),
            Outfit("dress_06", "秋香·拾穗", "秋香色的小裙子，口袋里装满了捡来的光。", OutfitCategory.DRESS, "outfit/dress_06.webp", 50),
            Outfit("museum_04", "花与静物", "荷兰黄金时代的一桌花开，比水壶更懂时间怎么慢慢流。", OutfitCategory.MUSEUM, "outfit/museum_04.webp", 60),
            Outfit("dress_07", "百期·绯缎", "第一百天的绯色缎面——能穿到它的人，早已把温柔穿在身上。", OutfitCategory.DRESS, "outfit/dress_07.webp", 100),
        )

    fun byId(id: String): Outfit? = all.firstOrNull { it.id == id }

    /** 连续达标 [streakDays] 天时已解锁的藏品，按 [Outfit.unlockDay] 升序。 */
    fun unlockedBy(streakDays: Int): List<Outfit> = all.filter { it.unlockDay <= streakDays }.sortedBy { it.unlockDay }
}
