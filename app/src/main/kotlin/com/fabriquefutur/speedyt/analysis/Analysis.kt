package com.fabriquefutur.speedyt.analysis

import com.fabriquefutur.speedyt.engine.SpeedTestResult
import kotlin.math.floor

/** What a connection can carry at the same time. */
data class Handles(
    val streams4k: Int,
    val streams1080p: Int,
    val hdVideoCalls: Int,
    val onlineGamingGood: Boolean,
)

fun handles(downloadMbps: Double, uploadMbps: Double, idleLatencyMs: Double, jitterMs: Double): Handles =
    Handles(
        streams4k = floor(downloadMbps / Thresholds.STREAM_4K_MBPS).toInt(),
        streams1080p = floor(downloadMbps / Thresholds.STREAM_1080P_MBPS).toInt(),
        // A call needs the rate both ways, so the slower direction decides.
        hdVideoCalls = floor(minOf(downloadMbps, uploadMbps) / Thresholds.VIDEO_CALL_MBPS).toInt(),
        onlineGamingGood = idleLatencyMs < Thresholds.GAMING_MAX_PING_MS &&
            jitterMs < Thresholds.GAMING_MAX_JITTER_MS,
    )

fun SpeedTestResult.handles(): Handles = handles(downloadMbps, uploadMbps, idleLatencyMs, jitterMs)

enum class BufferbloatGrade { A, B, C, D }

/** Grade from the extra latency a saturated link adds on top of the idle latency. */
fun bufferbloatGrade(idleLatencyMs: Double, loadedLatencyMs: Double): BufferbloatGrade {
    val added = (loadedLatencyMs - idleLatencyMs).coerceAtLeast(0.0)
    return when {
        added < Thresholds.BLOAT_A_BELOW_MS -> BufferbloatGrade.A
        added < Thresholds.BLOAT_B_BELOW_MS -> BufferbloatGrade.B
        added < Thresholds.BLOAT_C_BELOW_MS -> BufferbloatGrade.C
        else -> BufferbloatGrade.D
    }
}

/** The worse of the download and upload phases; null when neither measured latency under load. */
fun SpeedTestResult.bufferbloat(): BufferbloatGrade? {
    val loaded = listOfNotNull(downloadLoadedLatencyMs, uploadLoadedLatencyMs).maxOrNull() ?: return null
    return bufferbloatGrade(idleLatencyMs, loaded)
}

enum class ConnectionKind { ETHERNET, WIFI, OTHER, NONE }

enum class WifiBand { GHZ_2_4, GHZ_5, GHZ_6 }

/** Band from the channel frequency in MHz; null when the frequency is unknown or odd. */
fun wifiBand(frequencyMhz: Int?): WifiBand? = when (frequencyMhz) {
    null -> null
    in 2400..2500 -> WifiBand.GHZ_2_4
    in 4900..5924 -> WifiBand.GHZ_5
    in 5925..7125 -> WifiBand.GHZ_6
    else -> null
}

enum class SignalQuality { EXCELLENT, GOOD, FAIR, WEAK }

fun signalQuality(dbm: Int): SignalQuality = when {
    dbm >= Thresholds.SIGNAL_EXCELLENT_DBM -> SignalQuality.EXCELLENT
    dbm >= Thresholds.SIGNAL_GOOD_DBM -> SignalQuality.GOOD
    dbm >= Thresholds.SIGNAL_FAIR_DBM -> SignalQuality.FAIR
    else -> SignalQuality.WEAK
}

/** What the device knows about its link. No network name, no addresses. */
data class ConnectionInfo(
    val kind: ConnectionKind,
    val wifiFrequencyMhz: Int? = null,
    val signalDbm: Int? = null,
    val linkSpeedMbps: Int? = null,
) {
    val band: WifiBand? get() = wifiBand(wifiFrequencyMhz)
}

enum class Tip { USE_5_GHZ, WEAK_SIGNAL, ETHERNET_100_MBIT, WIFI_LINK_BOTTLENECK }

/** Advice that applies to this connection and this result, and nothing else. */
fun tips(connection: ConnectionInfo, downloadMbps: Double): List<Tip> = buildList {
    when (connection.kind) {
        ConnectionKind.WIFI -> {
            if (connection.band == WifiBand.GHZ_2_4) add(Tip.USE_5_GHZ)
            val dbm = connection.signalDbm
            if (dbm != null && dbm < Thresholds.SIGNAL_FAIR_DBM) add(Tip.WEAK_SIGNAL)
            val link = connection.linkSpeedMbps
            if (link != null && link > 0 && link < Thresholds.WIFI_LINK_FACTOR * downloadMbps) {
                add(Tip.WIFI_LINK_BOTTLENECK)
            }
        }
        ConnectionKind.ETHERNET -> {
            if (downloadMbps in Thresholds.ETHERNET_100M_MIN_MBPS..Thresholds.ETHERNET_100M_MAX_MBPS) {
                add(Tip.ETHERNET_100_MBIT)
            }
        }
        else -> Unit
    }
}
