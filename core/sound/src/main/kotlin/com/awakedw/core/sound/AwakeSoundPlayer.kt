package com.awakedw.core.sound

/**
 * 声音播放门面（fire-and-forget）。
 *
 * 实现契约：[play] 必须绝不抛异常——资源缺失（resId 解析不到）、静音、
 * 播放器未就绪或已释放等一律静默 no-op；调用方（记一杯等交互落点）无需任何防御。
 */
interface AwakeSoundPlayer {
    fun play(event: SoundEvent)
}
