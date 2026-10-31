package com.fabriquefutur.speedyt.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fabriquefutur.speedyt.R
import com.fabriquefutur.speedyt.analysis.ConnectionKind
import com.fabriquefutur.speedyt.analysis.SignalQuality
import com.fabriquefutur.speedyt.analysis.WifiBand
import com.fabriquefutur.speedyt.analysis.signalQuality
import com.fabriquefutur.speedyt.engine.ServerInfo
import java.util.Locale

fun formatMbps(v: Double): String = String.format(Locale.getDefault(), if (v >= 100) "%.0f" else "%.1f", v)

fun formatMs(v: Double): String = String.format(Locale.getDefault(), if (v >= 10) "%.0f" else "%.1f", v)

/**
 * "Madrid (MAD)", or the bare code for a data centre not in [COLO_CITIES].
 * Null without a code: Cloudflare's `city` and `country` headers locate the
 * viewer, not the server, so they never name it.
 */
fun serverName(server: ServerInfo?): String? {
    val colo = server?.colo?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: return null
    return COLO_CITIES[colo]?.let { "$it ($colo)" } ?: colo
}

@Composable
fun bandText(band: WifiBand): String = stringResource(
    when (band) {
        WifiBand.GHZ_2_4 -> R.string.band_2_4
        WifiBand.GHZ_5 -> R.string.band_5
        WifiBand.GHZ_6 -> R.string.band_6
    },
)

@Composable
fun kindText(kind: ConnectionKind): String = stringResource(
    when (kind) {
        ConnectionKind.ETHERNET -> R.string.connection_ethernet
        ConnectionKind.WIFI -> R.string.connection_wifi
        ConnectionKind.OTHER -> R.string.connection_other
        ConnectionKind.NONE -> R.string.connection_none
    },
)

/** "Ethernet", "Wi-Fi", or "Wi-Fi · 5 GHz". */
@Composable
fun connectionText(kind: ConnectionKind, band: WifiBand?): String =
    if (kind == ConnectionKind.WIFI && band != null) "${kindText(kind)} · ${bandText(band)}" else kindText(kind)

@Composable
fun signalText(dbm: Int): String {
    val word = stringResource(
        when (signalQuality(dbm)) {
            SignalQuality.EXCELLENT -> R.string.signal_excellent
            SignalQuality.GOOD -> R.string.signal_good
            SignalQuality.FAIR -> R.string.signal_fair
            SignalQuality.WEAK -> R.string.signal_weak
        },
    )
    return stringResource(R.string.signal_value, dbm, word)
}
