package com.fabriquefutur.speedyt.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ThroughputTest {
    private val sec = 1_000_000_000L

    /** 100 Mbps = 12.5 MB/s, sampled every 250 ms for [seconds]; the first [slowSeconds] run at 10 Mbps. */
    private fun ramp(seconds: Int, slowSeconds: Int): List<ByteSnapshot> {
        val out = mutableListOf(ByteSnapshot(0, 0))
        var bytes = 0L
        for (tick in 1..seconds * 4) {
            val slow = tick <= slowSeconds * 4
            bytes += if (slow) 312_500L else 3_125_000L // per 250 ms at 10 / 100 Mbps
            out += ByteSnapshot(tick * sec / 4, bytes)
        }
        return out
    }

    @Test fun steadyRateIsReadBackExactly() =
        assertEquals(100.0, Throughput.mbps(ramp(10, 0), 2 * sec), 1e-6)

    @Test fun warmUpIsDroppedSoTheSlowStartDoesNotLowerTheResult() {
        val s = ramp(10, 2)
        // Whole-run average would be 82 Mbps; after dropping 2 s it is the true 100.
        assertEquals(100.0, Throughput.mbps(s, 2 * sec), 1e-6)
        assertEquals(82.0, Throughput.mbps(s, 0), 1e-6)
    }

    @Test fun aPhaseShorterThanTheWarmUpFallsBackToTheWholeAverage() =
        assertEquals(100.0, Throughput.mbps(ramp(1, 0), 2 * sec), 1e-6)

    @Test fun fewerThanTwoSnapshotsIsZero() {
        assertEquals(0.0, Throughput.mbps(emptyList(), sec), 0.0)
        assertEquals(0.0, Throughput.mbps(listOf(ByteSnapshot(0, 0)), sec), 0.0)
    }

    @Test fun liveRateUsesOnlyTheLastWindow() {
        val s = ramp(10, 5) // 10 Mbps for 5 s, then 100 Mbps
        assertEquals(100.0, Throughput.liveMbps(s, sec), 1e-6)
        assertEquals(10.0, Throughput.liveMbps(s.take(9), sec), 1e-6)
    }
}
