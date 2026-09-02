package com.awakedw.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Navigation contract for the three water-tool tabs and first-use onboarding. */
class AwakeNavHostRoutingTest {
    @Test
    fun `unfinished onboarding starts at onboarding`() {
        assertEquals(AwakeDestination.Onboarding.route, startDestinationFor(onboardingDone = false))
    }

    @Test
    fun `completed onboarding starts at home`() {
        assertEquals(AwakeDestination.Home.route, startDestinationFor(onboardingDone = true))
    }

    @Test
    fun `onboarding does not show the bottom bar`() {
        assertFalse(showsBottomBar(AwakeDestination.Onboarding.route))
    }

    @Test
    fun `the three water-tool tabs show the bottom bar`() {
        assertTrue(showsBottomBar(AwakeDestination.Home.route))
        assertTrue(showsBottomBar(AwakeDestination.Stats.route))
        assertTrue(showsBottomBar(AwakeDestination.Settings.route))
    }

    @Test
    fun `unknown and empty routes do not show the bottom bar`() {
        assertFalse(showsBottomBar("somewhere_else"))
        assertFalse(showsBottomBar(null))
    }

    @Test
    fun `the four routes are unique`() {
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
