package com.tempodroid.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AstronomyRefreshSchedulerTest {

    @Test
    fun nextTriggerRoundsForwardToFiveMinuteBoundary() {
        val minute = 60_000L
        val hour = 60 * minute
        val now = 18 * hour + 57 * minute + 42_000L

        val next = AstronomyRefreshScheduler.nextTriggerAt(now)

        assertEquals(19 * hour, next)
    }

    @Test
    fun exactBoundaryAlwaysSchedulesFutureRefresh() {
        val minute = 60_000L
        val hour = 60 * minute
        val now = 19 * hour

        val next = AstronomyRefreshScheduler.nextTriggerAt(now)

        assertEquals(19 * hour + 5 * minute, next)
    }

    @Test
    fun scheduledGapNeverExceedsFiveMinutes() {
        val interval = AstronomyRefreshScheduler.INTERVAL_MILLIS
        listOf(0L, 1L, 59_999L, 13 * 60 * 60_000L + 31 * 60_000L, 86_399_999L)
            .forEach { now ->
                val next = AstronomyRefreshScheduler.nextTriggerAt(now)
                assertTrue(next > now)
                assertTrue(next - now <= interval)
            }
    }
}
