package com.fabriquefutur.speedyt.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.fabriquefutur.speedyt.R
import com.fabriquefutur.speedyt.data.HistoryEntry
import java.text.DateFormat
import java.util.Date

private const val W_DATE = 2.6f
private const val W_DOWN = 1.6f
private const val W_UP = 1.6f
private const val W_PING = 1.1f
private const val W_CONN = 2.2f

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryScreen(entries: List<HistoryEntry>) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(entries.isNotEmpty()) { if (entries.isNotEmpty()) runCatching { firstFocus.requestFocus() } }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = SafeHorizontal, vertical = SafeVertical),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.history_title), style = Type.Title)
        if (entries.isEmpty()) {
            Text(stringResource(R.string.history_empty), style = Type.Body.copy(color = SpeedytColors.Muted))
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            HeaderCell(R.string.history_col_date, W_DATE)
            HeaderCell(R.string.history_col_download, W_DOWN)
            HeaderCell(R.string.history_col_upload, W_UP)
            HeaderCell(R.string.history_col_ping, W_PING)
            HeaderCell(R.string.history_col_connection, W_CONN)
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(entries, key = { it.timestampMillis }) { e ->
                Surface(
                    onClick = {},
                    modifier = (if (e === entries.first()) Modifier.focusRequester(firstFocus) else Modifier).fillMaxWidth(),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = SpeedytColors.Card,
                        contentColor = SpeedytColors.Text,
                        focusedContainerColor = SpeedytColors.CardHigh,
                        focusedContentColor = SpeedytColors.Text,
                        pressedContainerColor = SpeedytColors.CardHigh,
                        pressedContentColor = SpeedytColors.Text,
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(3.dp, SpeedytColors.Focus)),
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cell(dateFormat.format(Date(e.timestampMillis)), W_DATE, Type.Body)
                        Cell(formatMbps(e.downloadMbps) + " " + stringResource(R.string.unit_mbps), W_DOWN, Type.BodyBold.copy(color = SpeedytColors.Download))
                        Cell(formatMbps(e.uploadMbps) + " " + stringResource(R.string.unit_mbps), W_UP, Type.BodyBold.copy(color = SpeedytColors.Upload))
                        Cell(formatMs(e.pingMs) + " " + stringResource(R.string.unit_ms), W_PING, Type.Body)
                        Cell(connectionText(e.connection, e.wifiBand), W_CONN, Type.Body)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(res: Int, weight: Float) {
    Text(stringResource(res), style = Type.Label, modifier = Modifier.weight(weight))
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(
    text: String,
    weight: Float,
    style: androidx.compose.ui.text.TextStyle,
) {
    Text(text, style = style, modifier = Modifier.weight(weight), maxLines = 1)
}
