package com.fabriquefutur.speedyt.engine

/** The stages of one test, in order. */
enum class Phase { LATENCY, DOWNLOAD, UPLOAD, DONE }

/**
 * Where the test server is, as the CDN reports it in response headers.
 * The client IP address is deliberately not part of this type: it is never read.
 */
data class ServerInfo(val city: String?, val colo: String?, val country: String?)

/** One latency probe: request sent to first response byte. */
data class PingResult(val millis: Double, val server: ServerInfo?)

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val idleLatencyMs: Double,
    val jitterMs: Double,
    val downloadLoadedLatencyMs: Double?,
    val uploadLoadedLatencyMs: Double?,
    val server: ServerInfo?,
)

/** A snapshot of a running test, for the screen. */
data class Progress(
    val phase: Phase,
    /** Current rate while a transfer phase runs (about the last second). */
    val liveMbps: Double = 0.0,
    /** 0..1 within the current phase. */
    val phaseFraction: Float = 0f,
    val latestPingMs: Double? = null,
    val idleLatencyMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val server: ServerInfo? = null,
)

data class EngineConfig(
    /** Latency probes after one discarded warm-up probe. */
    val idlePings: Int = 20,
    /** Length of the download phase and of the upload phase. */
    val phaseMillis: Long = 10_000,
    /** Bytes received or sent in this window are not counted. */
    val warmupMillis: Long = 2_000,
    val connections: Int = 6,
    /** How often a transfer phase publishes a live number. */
    val tickMillis: Long = 250,
    val loadedPingIntervalMillis: Long = 200,
    /**
     * Size asked of one download request; a worker starts another when it ends.
     * Cloudflare answers 403 from 100,000,000 bytes up, so stay well under it.
     */
    val downloadRequestBytes: Long = 50_000_000,
    /** Size of one upload request body. */
    val uploadRequestBytes: Long = 25_000_000,
    /** The live number is the average over this window. */
    val liveWindowMillis: Long = 1_000,
)
