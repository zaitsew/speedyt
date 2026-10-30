package com.fabriquefutur.speedyt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.fabriquefutur.speedyt.R
import com.fabriquefutur.speedyt.analysis.ConnectionInfo
import com.fabriquefutur.speedyt.engine.ErrorKind
import com.fabriquefutur.speedyt.engine.Phase
import com.fabriquefutur.speedyt.engine.Progress
import kotlin.math.ln

@Composable
fun HomeScreen(
    connection: ConnectionInfo,
    error: ErrorKind?,
    onStart: () -> Unit,
    onHistory: () -> Unit,
) {
    val startFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { startFocus.requestFocus() } }

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.app_name), style = Type.Hero)
            Text(stringResource(R.string.tagline), style = Type.Body.copy(color = SpeedytColors.Muted))
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PrimaryButton(
                    stringResource(R.string.start_test),
                    onStart,
                    Modifier.focusRequester(startFocus),
                )
                SecondaryButton(stringResource(R.string.history), onHistory)
            }
            Spacer(Modifier.height(24.dp))
            if (error != null) {
                Text(
                    stringResource(
                        when (error) {
                            ErrorKind.NO_NETWORK -> R.string.error_no_network
                            ErrorKind.TIMEOUT -> R.string.error_timeout
                            ErrorKind.FAILED -> R.string.error_failed
                        },
                    ),
                    style = Type.Body.copy(color = SpeedytColors.Error),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Text(
                stringResource(R.string.connected_via, connectionText(connection.kind, connection.band)),
                style = Type.Label,
            )
            Text(
                stringResource(R.string.privacy_note),
                style = Type.Label,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Gauge(
            fraction = 0f,
            color = SpeedytColors.Download,
            modifier = Modifier.size(300.dp),
        ) {
            Text(stringResource(R.string.unit_mbps), style = Type.Title.copy(color = SpeedytColors.Muted))
        }
    }
}

/** Dial position for a rate: logarithmic, so 10 Mbps and 1 Gbps both read well. Full scale is 1 Gbps. */
internal fun gaugeFraction(mbps: Double): Float =
    (ln(1.0 + mbps.coerceAtLeast(0.0)) / ln(1001.0)).toFloat().coerceIn(0f, 1f)

@Composable
fun RunningScreen(progress: Progress, onCancel: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { cancelFocus.requestFocus() } }

    val transferring = progress.phase == Phase.DOWNLOAD || progress.phase == Phase.UPLOAD
    val color = if (progress.phase == Phase.UPLOAD) SpeedytColors.Upload else SpeedytColors.Download
    val dialFraction = if (transferring) gaugeFraction(progress.liveMbps) else progress.phaseFraction
    val title = when (progress.phase) {
        Phase.LATENCY -> R.string.phase_latency
        Phase.DOWNLOAD -> R.string.phase_download
        Phase.UPLOAD, Phase.DONE -> R.string.phase_upload
    }

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Gauge(dialFraction, color, Modifier.size(340.dp)) {
            if (transferring) {
                NumberWithUnitColumn(formatMbps(progress.liveMbps), stringResource(R.string.unit_mbps), color)
            } else {
                NumberWithUnitColumn(
                    progress.latestPingMs?.let { formatMs(it) } ?: "–",
                    stringResource(R.string.unit_ms),
                    color,
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(title), style = Type.Title)
            ProgressBar(progress.phaseFraction, color)
            Spacer(Modifier.height(6.dp))
            StepRow(
                stringResource(R.string.ping),
                progress.idleLatencyMs?.let { formatMs(it) + " " + stringResource(R.string.unit_ms) },
                active = progress.phase == Phase.LATENCY,
            )
            StepRow(
                stringResource(R.string.download),
                progress.downloadMbps?.let { formatMbps(it) + " " + stringResource(R.string.unit_mbps) },
                active = progress.phase == Phase.DOWNLOAD,
            )
            StepRow(
                stringResource(R.string.upload),
                progress.uploadMbps?.let { formatMbps(it) + " " + stringResource(R.string.unit_mbps) },
                active = progress.phase == Phase.UPLOAD,
            )
            serverName(progress.server)?.let {
                Text(stringResource(R.string.server_label, it), style = Type.Label)
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton(stringResource(R.string.cancel), onCancel, Modifier.focusRequester(cancelFocus))
        }
    }
}

@Composable
private fun NumberWithUnitColumn(number: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number, style = Type.Hero, color = color)
        Text(unit, style = Type.Section)
    }
}

@Composable
private fun StepRow(label: String, value: String?, active: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (active) Type.BodyBold else Type.Body.copy(color = SpeedytColors.Muted),
            modifier = Modifier.weight(1f),
        )
        Text(value ?: if (active) "…" else "–", style = if (active) Type.BodyBold else Type.Body)
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(SpeedytColors.Track),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color),
        )
    }
}
