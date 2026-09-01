package com.awakedw.core.sound

/**
 * 可播放音效枚举。
 *
 * [rawName] 即 `res/raw/` 下的音频资源名契约（如 `res/raw/drop_a.ogg` → rawName="drop_a"），
 * 播放器据此以 `resources.getIdentifier(rawName, "raw", packageName)` 解析 resId。
 * 音频文件由用户按计划附录 B 后补；资源缺失时实现必须静默 no-op，不得抛异常。
 */
enum class SoundEvent(val rawName: String) {
    DROP_A("drop_a"),
    DROP_B("drop_b"),
    DROP_C("drop_c"),
    GOAL_MELODY("goal_melody"),
    PURR("purr"),
}
