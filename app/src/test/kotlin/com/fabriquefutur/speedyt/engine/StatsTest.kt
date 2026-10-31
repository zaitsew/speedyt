package com.fabriquefutur.speedyt.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsTest {
    @Test fun medianOfOddCountIsTheMiddleValue() =
        assertEquals(12.0, Stats.median(listOf(30.0, 12.0, 8.0))!!, 1e-9)

    @Test fun medianOfEvenCountIsTheMeanOfTheTwoMiddleValues() =
        assertEquals(15.0, Stats.median(listOf(10.0, 40.0, 20.0, 5.0))!!, 1e-9)

    @Test fun medianIgnoresOneSlowOutlier() =
        assertEquals(10.0, Stats.median(listOf(10.0, 10.0, 10.0, 10.0, 900.0))!!, 1e-9)

    @Test fun medianOfNothingIsNull() = assertNull(Stats.median(emptyList()))

    @Test fun jitterIsTheMeanAbsoluteDifferenceOfConsecutiveSamples() {
        // |12-10| = 2, |9-12| = 3, |15-9| = 6 -> 11 / 3
        assertEquals(11.0 / 3.0, Stats.jitter(listOf(10.0, 12.0, 9.0, 15.0)), 1e-9)
    }

    @Test fun jitterOfSteadySamplesIsZero() =
        assertEquals(0.0, Stats.jitter(listOf(20.0, 20.0, 20.0)), 1e-9)

    @Test fun jitterNeedsTwoSamples() {
        assertEquals(0.0, Stats.jitter(emptyList()), 0.0)
        assertEquals(0.0, Stats.jitter(listOf(5.0)), 0.0)
    }
}
