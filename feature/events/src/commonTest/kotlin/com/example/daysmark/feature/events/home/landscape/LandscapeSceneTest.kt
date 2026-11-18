package com.example.daysmark.feature.events.home.landscape

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LandscapeSceneTest {
    private fun scene(past: Int, future: Int) =
        LandscapeScene(width = 350.0, height = 96.0, pastCount = past, futureCount = future)

    @Test
    fun withoutEventsOnlyTodayRemains() {
        val scene = scene(past = 0, future = 0)
        assertNull(scene.pastHill)
        assertNull(scene.futureHill)
        assertEquals(emptyList(), scene.trees)
        assertEquals(175.0, scene.center)
    }

    @Test
    fun eachSideAppearsOnlyWithItsEvents() {
        val futureOnly = scene(past = 0, future = 2)
        assertNull(futureOnly.pastHill)
        assertTrue(futureOnly.trees.none { it.isPast })
        assertEquals(emptyList(), futureOnly.leaves)

        val pastOnly = scene(past = 2, future = 0)
        assertNull(pastOnly.futureHill)
        assertNull(pastOnly.distantHill)
        assertEquals(3, pastOnly.leaves.size)
    }

    @Test
    fun hillsGrowWithEventsUpToEight() {
        val one = scene(past = 1, future = 1).futureHill!!.height
        val eight = scene(past = 8, future = 8).futureHill!!.height
        val twenty = scene(past = 20, future = 20).futureHill!!.height
        assertTrue(one < eight)
        assertEquals(eight, twenty)
        assertEquals(34.0, eight, absoluteTolerance = 1e-9)
    }

    @Test
    fun treesStandOnTheHillNotBelowTheGround() {
        val scene = scene(past = 3, future = 3)
        assertTrue(scene.trees.all { it.baseline <= scene.ground + scene.artScale })
    }
}
