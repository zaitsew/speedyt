package com.fabriquefutur.speedyt.engine

import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs one speed test: idle latency, download, upload, and latency under load.
 * Pure Kotlin; the network sits behind [SpeedTransport] and the clock is
 * injected, so tests run in virtual time.
 */
class SpeedTestEngine(
    private val transport: SpeedTransport,
    private val config: EngineConfig = EngineConfig(),
    private val nanoClock: () -> Long = System::nanoTime,
) {
    private class Idle(val medianMs: Double, val jitterMs: Double, val server: ServerInfo?)
    private class Phased(val mbps: Double, val loadedLatencyMs: Double?)

    suspend fun run(onProgress: (Progress) -> Unit = {}): SpeedTestResult {
        val idle = measureIdle(onProgress)
        val base = Progress(Phase.DOWNLOAD, idleLatencyMs = idle.medianMs, server = idle.server)

        val down = measureTransfer(base.copy(phase = Phase.DOWNLOAD), onProgress) { onBytes ->
            transport.download(config.downloadRequestBytes, onBytes)
        }
        val up = measureTransfer(
            base.copy(phase = Phase.UPLOAD, downloadMbps = down.mbps), onProgress,
        ) { onBytes ->
            transport.upload(config.uploadRequestBytes, onBytes)
        }

        onProgress(
            base.copy(phase = Phase.DONE, phaseFraction = 1f, downloadMbps = down.mbps, uploadMbps = up.mbps),
        )
        return SpeedTestResult(
            downloadMbps = down.mbps,
            uploadMbps = up.mbps,
            idleLatencyMs = idle.medianMs,
            jitterMs = idle.jitterMs,
            downloadLoadedLatencyMs = down.loadedLatencyMs,
            uploadLoadedLatencyMs = up.loadedLatencyMs,
            server = idle.server,
        )
    }

    private suspend fun measureIdle(onProgress: (Progress) -> Unit): Idle {
        onProgress(Progress(Phase.LATENCY))
        // The first probe pays for DNS, TCP and TLS. It is not counted, and if
        // it fails there is no network worth testing.
        var server: ServerInfo? = try {
            transport.ping().server
        } catch (e: IOException) {
            currentCoroutineContext().ensureActive()
            throw SpeedTestException(classifyError(e), e)
        }

        val samples = ArrayList<Double>()
        var lastError: IOException? = null
        repeat(config.idlePings) { i ->
            try {
                val p = transport.ping()
                samples += p.millis
                server = p.server ?: server
                onProgress(
                    Progress(
                        Phase.LATENCY,
                        phaseFraction = (i + 1f) / config.idlePings,
                        latestPingMs = p.millis,
                        server = server,
                    ),
                )
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                lastError = e
            }
        }
        val median = Stats.median(samples) ?: throw SpeedTestException(classifyError(lastError), lastError)
        return Idle(median, Stats.jitter(samples), server)
    }

    /**
     * Runs [connections][EngineConfig.connections] workers that each repeat
     * [worker] until the phase ends, while a sampler counts bytes and a prober
     * measures latency under load.
     */
    private suspend fun measureTransfer(
        base: Progress,
        onProgress: (Progress) -> Unit,
        worker: suspend (onBytes: (Int) -> Unit) -> Unit,
    ): Phased {
        val total = AtomicLong(0)
        val failure = AtomicReference<IOException?>(null)
        val loaded = Collections.synchronizedList(ArrayList<Double>())
        val snapshots = ArrayList<ByteSnapshot>()
        val warmupNanos = config.warmupMillis * NANOS_PER_MILLI
        val phaseNanos = config.phaseMillis * NANOS_PER_MILLI
        val liveWindowNanos = config.liveWindowMillis * NANOS_PER_MILLI

        val start = nanoClock()
        // Every worker stopped on an error before the phase ended: the bytes so far
        // describe a broken link, not its speed.
        var allFailed = false
        snapshots += ByteSnapshot(start, 0)
        onProgress(base)

        coroutineScope {
            val workers = List(config.connections) {
                launch {
                    while (isActive) {
                        try {
                            worker { total.addAndGet(it.toLong()) }
                        } catch (e: IOException) {
                            ensureActive()
                            failure.compareAndSet(null, e)
                            break
                        }
                    }
                }
            }
            // Latency under load: only probes after the warm-up count, like the bytes.
            launch {
                while (isActive) {
                    try {
                        val p = transport.ping()
                        if (nanoClock() - start >= warmupNanos) loaded += p.millis
                    } catch (e: IOException) {
                        ensureActive()
                    }
                    delay(config.loadedPingIntervalMillis)
                }
            }

            while (true) {
                delay(config.tickMillis)
                val now = nanoClock()
                snapshots += ByteSnapshot(now, total.get())
                onProgress(
                    base.copy(
                        liveMbps = Throughput.liveMbps(snapshots, liveWindowNanos),
                        phaseFraction = ((now - start).toFloat() / phaseNanos).coerceIn(0f, 1f),
                    ),
                )
                if (now - start >= phaseNanos) break
                if (workers.all { it.isCompleted }) {
                    allFailed = true
                    break
                }
            }
            currentCoroutineContext().cancelChildren()
        }

        if (total.get() == 0L || allFailed) {
            val e = failure.get()
            throw SpeedTestException(classifyError(e), e)
        }
        return Phased(Throughput.mbps(snapshots, warmupNanos), Stats.median(loaded.toList()))
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
