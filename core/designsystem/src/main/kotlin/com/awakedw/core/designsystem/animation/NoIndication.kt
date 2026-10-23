package com.awakedw.core.designsystem.animation

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

/**
 * 「无水波纹」原语（§24 按压语言的水波纹退役）：给 M3 组件
 * （NavigationBarItem 等不暴露 indication 形参的）经
 * `CompositionLocalProvider(LocalIndication provides NoIndication)` 关闭库存涟漪，
 * 按压回应交给 [pressScale]。单例恒等——IndicationNodeFactory 以相等性做缓存键，
 * 单例可让同区域内所有命中组件共享同一无操作节点。
 */
object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}

    // 缓存按工厂相等性去重：单例恒等即恒命中同一无操作节点。
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
