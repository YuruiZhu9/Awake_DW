package com.awakedw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 导航壳路由/分支判定（规格 §3 + 集成决议，纯 JVM）：
 * - 启动分支：onboardingDone()==false 先入引导路由（且底栏隐藏），true 正常进首页；
 * - 底栏显隐：仅首页/统计/我的三个主页签显示，引导路由一律无底栏；
 * - 路由常量：三主页签 + 引导共四条路由，字符串互异。
 */
class AwakeNavHostRoutingTest {
    @Test
    fun `未完成引导_启动分支先入引导路由`() {
        assertEquals(AwakeDestination.Onboarding.route, startDestinationFor(onboardingDone = false))
    }

    @Test
    fun `已完成引导_启动分支正常进首页`() {
        assertEquals(AwakeDestination.Home.route, startDestinationFor(onboardingDone = true))
    }

    @Test
    fun `引导路由不显示底栏`() {
        assertFalse(showsBottomBar(AwakeDestination.Onboarding.route))
    }

    @Test
    fun `三个主页签显示底栏`() {
        assertTrue(showsBottomBar(AwakeDestination.Home.route))
        assertTrue(showsBottomBar(AwakeDestination.Stats.route))
        assertTrue(showsBottomBar(AwakeDestination.Settings.route))
    }

    @Test
    fun `未知路由与空路由不显示底栏`() {
        assertFalse(showsBottomBar("somewhere_else"))
        assertFalse(showsBottomBar(null))
    }

    @Test
    fun `四条路由字符串互异`() {
        val routes =
            listOf(
                AwakeDestination.Home.route,
                AwakeDestination.Stats.route,
                AwakeDestination.Settings.route,
                AwakeDestination.Onboarding.route,
            )
        assertEquals(routes.size, routes.toSet().size)
    }
}
