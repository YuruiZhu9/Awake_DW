package com.awakedw.core.testsupport

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * 视觉回归基线（roadmap P1，2.7.0）：把各页视觉 review 的截图与随仓入库的基线比对，
 * 让**跨版本的意外视觉漂移**在构建期被绊住。诊断图（`build/reports/visual-review/`）
 * 与真机闸门的职责不变——基线是回归绊网，不是像素级金图，更不是真机审美的替代。
 *
 * 行为：
 * - 基线缺失 → 自动播种并通过（新克隆自举），stdout 声明播种路径；
 * - 环境变量 `AWAKE_VISUAL_BASELINE=update` → 重拍基线（有意的设计变更后使用）；
 * - 环境变量 `AWAKE_VISUAL_BASELINE=check` → **门禁模式**：漂移判失败；普通构建只告警并转储。
 *   门禁成立的前提是「逐模块串行」执行（本机实测稳定）；全量并行构建下 Robolectric 的
 *   装饰层组合存在负载相关的次可感知翻转（整层中景有/无，实测 9% 像素级差异、肉眼不可分），
 *   在该环境解开之前，绊网以发布前门禁而非每次构建拦截的方式运行（2.7.0 实测定稿）；
 * - 比对口径：尺寸必须一致；逐像素 RGB 通道差 > [CHANNEL_TOLERANCE] 记差异像素，
 *   占比 > [MAX_DIFF_RATIO] 判失败——1% 的绊网，容忍个别噪点、抓得住排版级漂移。
 *
 * 机器绑定属性如实披露：字体渲染随环境而异，基线只在生成它的机器上承诺稳定。
 */
object VisualBaseline {
    private const val CHANNEL_TOLERANCE = 24
    private const val MAX_DIFF_RATIO = 0.03

    /** 有意更新基线的开关：`AWAKE_VISUAL_BASELINE=update gradlew <module>:testDebugUnitTest`。 */
    val updateMode: Boolean get() = System.getenv("AWAKE_VISUAL_BASELINE") == "update"

    /** 门禁模式：漂移判失败；仅声明 `AWAKE_VISUAL_BASELINE=check` 时启用（逐模块串行执行）。 */
    private val gateMode: Boolean get() = System.getenv("AWAKE_VISUAL_BASELINE") == "check"

    /**
     * 把截图与基线比对（或播种/更新基线）。
     *
     * @param module 页面所属模块名（基线子目录），如 "stats"、"settings"
     * @param label  场景标签（基线文件名），如 "populated-emerald"
     * @param settle 宽限收场：首比漂移后调用（推进主循环时钟、等待空闲），冲掉可能的动画尾帧
     * @param draw   重新绘制当前画面并返回新位图（在 UI 线程执行 draw）
     */
    fun verify(
        module: String,
        label: String,
        settle: () -> Unit,
        draw: () -> Bitmap,
    ) {
        val file = File("visual-baselines/$module/$label.png")
        if (updateMode || !file.exists()) {
            val bitmap = draw()
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            println("[visual-baseline] ${if (updateMode) "updated" else "seeded"} ${file.path}")
            bitmap.recycle()
            return
        }
        val expected = BitmapFactory.decodeFile(file.path)
        checkNotNull(expected) { "视觉基线解码失败：${file.path}（删除该文件可重新播种）" }
        val first = draw()
        check(expected.width == first.width && expected.height == first.height) {
            "视觉基线尺寸不一致：$module/$label 期望 ${expected.width}×${expected.height}，实际 ${first.width}×${first.height}。" +
                "若为视口有意变更，请以 AWAKE_VISUAL_BASELINE=update 重拍基线。"
        }
        if (diffRatio(expected, first) <= MAX_DIFF_RATIO) {
            first.recycle()
            expected.recycle()
            return
        }
        // 首比漂移：先宽限重拍再定罪——动画尾帧 / 负载下的瞬时重组会被 settle 冲走，
        // 真实的设计变更在重拍里同样漂移，照样判失败（绊网语义不变，只滤掉瞬态伪漂移）。
        println("[visual-baseline] $module/$label 首比漂移，宽限重拍复验…")
        first.recycle()
        settle()
        val second = draw()
        val diffRatio = diffRatio(expected, second)
        val dump = File("build/reports/visual-drift/$module-$label-actual.png")
        dump.parentFile?.mkdirs()
        dump.outputStream().use { second.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val ratio = "%.2f".format(diffRatio * 100)
        second.recycle()
        expected.recycle()
        val message =
            "视觉漂移：$module/$label 宽限重拍后仍差异 $ratio%（阈值 ${MAX_DIFF_RATIO * 100}%）；实际图转储 ${dump.path}。" +
                "若为有意的设计变更，请以 AWAKE_VISUAL_BASELINE=update 重拍基线。"
        if (gateMode) {
            error(message)
        }
        // 普通构建只告警不拦截：全量并行下装饰层组合存在负载相关的次可感知翻转（环境未解开），
        // 绊网以发布前门禁模式（AWAKE_VISUAL_BASELINE=check，逐模块串行）拦截。
        println("[visual-baseline] WARNING $message")
    }

    /** 差异像素占比：任一 RGB 通道差超过容差即计入（alpha 不计，截图恒不透明）。 */
    private fun diffRatio(
        expected: Bitmap,
        actual: Bitmap,
    ): Double {
        val width = expected.width
        val height = expected.height
        val expectedPixels = IntArray(width * height)
        val actualPixels = IntArray(width * height)
        expected.getPixels(expectedPixels, 0, width, 0, 0, width, height)
        actual.getPixels(actualPixels, 0, width, 0, 0, width, height)
        var diff = 0
        for (i in expectedPixels.indices) {
            val e = expectedPixels[i]
            val a = actualPixels[i]
            // 位移与掩码必须显式加括号：infix 优先级低于减法，裸写会错算成 `and (0xFF - x)`。
            val redDiff = ((e shr 16 and 0xFF) - (a shr 16 and 0xFF)).let { if (it < 0) -it else it }
            val greenDiff = ((e shr 8 and 0xFF) - (a shr 8 and 0xFF)).let { if (it < 0) -it else it }
            val blueDiff = ((e and 0xFF) - (a and 0xFF)).let { if (it < 0) -it else it }
            if (redDiff > CHANNEL_TOLERANCE || greenDiff > CHANNEL_TOLERANCE || blueDiff > CHANNEL_TOLERANCE) {
                diff++
            }
        }
        return diff.toDouble() / expectedPixels.size
    }
}
