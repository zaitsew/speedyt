package com.fabriquefutur.speedyt.engine

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay

/**
 * A network with a known speed. Time is the test scheduler's virtual time, so
 * a 10 s phase runs instantly and the numbers are exact.
 */
class FakeTransport(
    private val downMbps: Double = 80.0,
    private val upMbps: Double = 20.0,
    private val idleMs: Long = 10,
    private val downLoadedMs: Long = 50,
    private val upLoadedMs: Long = 200,
    private val server: ServerInfo? = ServerInfo("Madrid", "MAD", "ES"),
    /** Extra connection-level failure to throw from every call. */
    private val failWith: IOException? = null,
    /** Idle pings answered with these latencies, in order, before settling on [idleMs]. */
    private val idleScript: List<Long> = emptyList(),
) : SpeedTransport {
    @Volatile private var downloading = 0
    @Volatile private var uploading = 0
    private val pingCalls = AtomicInteger(0)
    val downloadCalls = AtomicInteger(0)
    val uploadCalls = AtomicInteger(0)

    override suspend fun ping(): PingResult {
        failWith?.let { throw it }
        val n = pingCalls.getAndIncrement()
        val ms = when {
            uploading > 0 -> upLoadedMs
            downloading > 0 -> downLoadedMs
            n in idleScript.indices -> idleScript[n]
            else -> idleMs
        }
        delay(ms)
        return PingResult(ms.toDouble(), server)
    }

    override suspend fun download(bytes: Long, onBytes: (Int) -> Unit) = stream(bytes, downMbps, onBytes, down = true)

    override suspend fun upload(bytes: Long, onBytes: (Int) -> Unit) = stream(bytes, upMbps, onBytes, down = false)

    private suspend fun stream(bytes: Long, mbps: Double, onBytes: (Int) -> Unit, down: Boolean) {
        failWith?.let { throw it }
        (if (down) downloadCalls else uploadCalls).incrementAndGet()
        // The rate is shared by every parallel connection: each carries its slice.
        val perConnection = mbps / CONNECTIONS
        val chunkMillis = 10L
        val chunk = (perConnection * 1_000_000 / 8 * chunkMillis / 1000).toInt()
        if (down) downloading++ else uploading++
        try {
            var left = bytes
            while (left > 0) {
                delay(chunkMillis)
                onBytes(chunk)
                left -= chunk
            }
        } finally {
            if (down) downloading-- else uploading--
        }
    }

    private companion object {
        const val CONNECTIONS = 6
    }
}
