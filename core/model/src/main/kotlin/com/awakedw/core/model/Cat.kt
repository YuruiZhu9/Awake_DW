package com.awakedw.core.model

/**
 * 胆大王的心情三态（moodboard §6）：由庆祝时刻与当前小时纯函数推导，见 [resolveCatMood]。
 */
enum class CatMood { IDLE, HAPPY, SLEEPY }

/**
 * 胆大王的配饰藏品。[unlockDay] 为连续达标天数门槛（与 OutfitCatalog 解锁曲线同语义）；
 * [assetFile] 为 :app assets 相对路径，缺失时由表现层矢量兜底（资产后补即生效）。
 */
enum class CatAccessory(
    val unlockDay: Int,
    val assetFile: String,
) {
    BOW(3, "cat/acc_bow.webp"),
    PEARL(14, "cat/acc_pearl.webp"),
    OUTFIT(30, "cat/acc_dress.webp"),
}

/** 连续达标 [streakDays] 天时已解锁的配饰，按 [CatAccessory.unlockDay] 升序。 */
fun unlockedCatAccessories(streakDays: Int): List<CatAccessory> =
    CatAccessory.entries.filter { it.unlockDay <= streakDays }.sortedBy { it.unlockDay }

/**
 * 心情判定（治愈铁律：零惩罚，无失败）：
 * 刚庆祝 → HAPPY（庆祝优先于困意）；本地深夜窗 [22, 6) → SLEEPY（与 ResolveThemeUseCase 深夜窗同源，NIGHT_END_HOUR=6 不含，06:00 起算白天）；否则 IDLE。
 */
fun resolveCatMood(
    justCelebrated: Boolean,
    nowHour: Int,
): CatMood =
    when {
        justCelebrated -> CatMood.HAPPY
        nowHour >= 22 || nowHour < 6 -> CatMood.SLEEPY
        else -> CatMood.IDLE
    }
