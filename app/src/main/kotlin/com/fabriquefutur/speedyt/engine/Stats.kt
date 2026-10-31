package com.fabriquefutur.speedyt.engine

/** Cumulative byte count at a moment, taken by the engine every tick. */
data class ByteSnapshot(val nanos: Long, val totalBytes: Long)

object Stats {
    /** Median of [values], or null when there are none. */
    fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
    }

    /** Jitter: mean absolute difference of consecutive samples. Zero for fewer than two. */
    fun jitter(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        var sum = 0.0
        for (i in 1 until values.size) sum += kotlin.math.abs(values[i] - values[i - 1])
        return sum / (values.size - 1)
    }
}

object Throughput {
    private const val NANOS_PER_SECOND = 1_000_000_000.0

    /**
     * Mbps over the whole phase after dropping the first [warmupNanos].
     *
     * The first snapshot marks the start of the phase. The rate runs from the
     * first snapshot at or after the end of the warm-up to the last one, so
     * slow-start ramp-up never lowers the figure. When the phase was shorter
     * than the warm-up, or has no later snapshot, the whole-phase average is
     * returned instead.
     */
    fun mbps(snapshots: List<ByteSnapshot>, warmupNanos: Long): Double {
        if (snapshots.size < 2) return 0.0
        val first = snapshots.first()
        val last = snapshots.last()
        val warmEnd = first.nanos + warmupNanos
        val from = snapshots.firstOrNull { it.nanos >= warmEnd && it !== last } ?: first
        return rate(from, last)
    }

    /** Mbps over the most recent [windowNanos], for the live number. */
    fun liveMbps(snapshots: List<ByteSnapshot>, windowNanos: Long): Double {
        if (snapshots.size < 2) return 0.0
        val last = snapshots.last()
        val from = snapshots.firstOrNull { it.nanos >= last.nanos - windowNanos && it !== last } ?: return 0.0
        return rate(from, last)
    }

    private fun rate(from: ByteSnapshot, to: ByteSnapshot): Double {
        val seconds = (to.nanos - from.nanos) / NANOS_PER_SECOND
        if (seconds <= 0.0) return 0.0
        return (to.totalBytes - from.totalBytes) * 8.0 / seconds / 1_000_000.0
    }
}
