package com.fabriquefutur.speedyt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.fabriquefutur.speedyt.R
import com.fabriquefutur.speedyt.analysis.BufferbloatGrade
import com.fabriquefutur.speedyt.analysis.ConnectionInfo
import com.fabriquefutur.speedyt.analysis.ConnectionKind
import com.fabriquefutur.speedyt.analysis.Tip
import com.fabriquefutur.speedyt.analysis.bufferbloat
import com.fabriquefutur.speedyt.analysis.handles
import com.fabriquefutur.speedyt.analysis.tips
import com.fabriquefutur.speedyt.engine.SpeedTestResult

@Composable
fun ResultsScreen(
    result: SpeedTestResult,
    connection: ConnectionInfo,
    onRunAgain: () -> Unit,
    onHistory: () -> Unit,
) {
    val runFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { runFocus.requestFocus() } }
    val unitMbps = stringResource(R.string.unit_mbps)
    val unitMs = stringResource(R.string.unit_ms)
    val handles = remember(result) { result.handles() }
    val grade = remember(result) { result.bufferbloat() }
    val tipList = remember(result, connection) { tips(connection, result.downloadMbps) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.results_title), style = Type.Title)
                serverName(result.server)?.let {
                    Text(stringResource(R.string.server_label, it), style = Type.Label)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton(stringResource(R.string.run_again), onRunAgain, Modifier.focusRequester(runFocus))
                SecondaryButton(stringResource(R.string.history), onHistory)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroCard(stringResource(R.string.download), formatMbps(result.downloadMbps), unitMbps, SpeedytColors.Download, Modifier.weight(1f))
            HeroCard(stringResource(R.string.upload), formatMbps(result.uploadMbps), unitMbps, SpeedytColors.Upload, Modifier.weight(1f))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(stringResource(R.string.ping), formatMs(result.idleLatencyMs), unitMs, Modifier.weight(1f))
            StatCard(stringResource(R.string.jitter), formatMs(result.jitterMs), unitMs, Modifier.weight(1f))
            StatCard(
                stringResource(R.string.ping_loaded_download),
                result.downloadLoadedLatencyMs?.let { formatMs(it) } ?: "–", unitMs, Modifier.weight(1f),
            )
            StatCard(
                stringResource(R.string.ping_loaded_upload),
                result.uploadLoadedLatencyMs?.let { formatMs(it) } ?: "–", unitMs, Modifier.weight(1f),
            )
            BufferbloatCard(grade, Modifier.weight(1.7f))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FocusPanel(Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.handles_title), style = Type.Section)
                    ValueRow(stringResource(R.string.handles_4k), handles.streams4k.toString())
                    ValueRow(stringResource(R.string.handles_1080p), handles.streams1080p.toString())
                    ValueRow(stringResource(R.string.handles_calls), handles.hdVideoCalls.toString())
                    ValueRow(
                        stringResource(R.string.handles_gaming),
                        stringResource(if (handles.onlineGamingGood) R.string.gaming_good else R.string.gaming_poor),
                        if (handles.onlineGamingGood) SpeedytColors.grade(BufferbloatGrade.A) else SpeedytColors.grade(BufferbloatGrade.C),
                    )
                }
            }
            FocusPanel(Modifier.weight(1f)) {
                ConnectionPanel(connection)
            }
        }

        if (tipList.isNotEmpty()) {
            FocusPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tips_title), style = Type.Section)
                    tipList.forEach { tip -> Text(tipText(tip, connection), style = Type.Body) }
                }
            }
        }
    }
}

@Composable
private fun tipText(tip: Tip, connection: ConnectionInfo): String = when (tip) {
    Tip.USE_5_GHZ -> stringResource(R.string.tip_5ghz)
    Tip.WEAK_SIGNAL -> stringResource(R.string.tip_weak_signal, connection.signalDbm ?: 0)
    Tip.ETHERNET_100_MBIT -> stringResource(R.string.tip_ethernet_100)
    Tip.WIFI_LINK_BOTTLENECK -> stringResource(R.string.tip_wifi_link, connection.linkSpeedMbps ?: 0)
}

@Composable
private fun HeroCard(label: String, number: String, unit: String, color: Color, modifier: Modifier) {
    FocusPanel(modifier) {
        Column {
            Text(label, style = Type.Section)
            NumberWithUnit(number, unit, Type.Hero, color)
        }
    }
}

@Composable
private fun StatCard(label: String, number: String, unit: String, modifier: Modifier) {
    FocusPanel(modifier) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = Type.Label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f, fill = false).height(4.dp))
            NumberWithUnit(number, unit, Type.Metric)
        }
    }
}

@Composable
private fun BufferbloatCard(grade: BufferbloatGrade?, modifier: Modifier) {
    FocusPanel(modifier) {
        Column {
            Text(stringResource(R.string.bufferbloat), style = Type.Label)
            if (grade == null) {
                Text("–", style = Type.Metric)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(grade.name, style = Type.Hero.copy(), color = SpeedytColors.grade(grade))
                    Text(
                        stringResource(
                            when (grade) {
                                BufferbloatGrade.A -> R.string.bufferbloat_a
                                BufferbloatGrade.B -> R.string.bufferbloat_b
                                BufferbloatGrade.C -> R.string.bufferbloat_c
                                BufferbloatGrade.D -> R.string.bufferbloat_d
                            },
                        ),
                        style = Type.Label.copy(color = SpeedytColors.Text),
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String, valueColor: Color = SpeedytColors.Text) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.Body, modifier = Modifier.weight(1f))
        Text(value, style = Type.BodyBold, color = valueColor)
    }
}

@Composable
private fun ConnectionPanel(connection: ConnectionInfo) {
    val unknown = stringResource(R.string.unknown_value)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.connection_title), style = Type.Section)
        ValueRow(stringResource(R.string.connection_type), kindText(connection.kind))
        if (connection.kind == ConnectionKind.WIFI) {
            ValueRow(stringResource(R.string.connection_band), connection.band?.let { bandText(it) } ?: unknown)
            ValueRow(
                stringResource(R.string.connection_signal),
                connection.signalDbm?.let { signalText(it) } ?: unknown,
            )
            ValueRow(
                stringResource(R.string.connection_link),
                connection.linkSpeedMbps?.let { stringResource(R.string.link_value, it) } ?: unknown,
            )
        }
    }
}
