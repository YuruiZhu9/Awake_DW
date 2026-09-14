package com.awakedw.core.model

/**
 * 打卡确认语：一句正文，加一行可选的落款。
 *
 * 形态取自取餐小票上的引文——正文是那句要说的话，落款是它从哪儿来：
 *
 * - [text] 正文，2–14 字。古典诗文摘句与原创短句共用同一字段。
 * - [attribution] 落款（作者名，不带破折号）；`null` 表示这是应用自己写的一句话，
 *   **没有出处可署，就不署**——不写「佚名」、不编作者。
 *
 * 有内容无落款是合法形态；只写落款不写内容不是（正文为空时调用方不应展示）。
 */
data class PraiseQuote(
    val text: String,
    val attribution: String? = null,
)
