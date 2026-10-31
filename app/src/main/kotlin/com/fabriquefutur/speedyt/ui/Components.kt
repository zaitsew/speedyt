package com.fabriquefutur.speedyt.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

/** Type scale, sized for reading from about 3 m. Nothing is smaller than 16 sp. */
object Type {
    val Hero = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Bold, color = SpeedytColors.Text)
    val Metric = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = SpeedytColors.Text)
    val Title = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = SpeedytColors.Text)
    val Section = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = SpeedytColors.Muted)
    val Body = TextStyle(fontSize = 20.sp, color = SpeedytColors.Text)
    val BodyBold = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpeedytColors.Text)
    val Label = TextStyle(fontSize = 16.sp, color = SpeedytColors.Muted)
    val Unit = TextStyle(fontSize = 20.sp, color = SpeedytColors.Muted)
}

private val CardShape = RoundedCornerShape(16.dp)

/**
 * A panel that takes D-pad focus and shows it with a bright border. Panels
 * that only display numbers are focusable too, so the D-pad can walk down a
 * screen that is taller than the TV and scroll it.
 */
@Composable
fun FocusPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .clip(CardShape)
            .background(if (focused) SpeedytColors.CardHigh else SpeedytColors.Card)
            .border(3.dp, if (focused) SpeedytColors.Focus else Color.Transparent, CardShape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(16.dp),
        content = content,
    )
}

/** A number with its unit, baseline-aligned. */
@Composable
fun NumberWithUnit(number: String, unit: String, numberStyle: TextStyle, color: Color = SpeedytColors.Text) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(number, style = numberStyle, color = color)
        Text(unit, style = Type.Unit, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = SpeedytColors.Download,
            contentColor = SpeedytColors.Background,
            focusedContainerColor = SpeedytColors.Download,
            focusedContentColor = SpeedytColors.Background,
            pressedContainerColor = SpeedytColors.Download,
            pressedContentColor = SpeedytColors.Background,
        ),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(4.dp, SpeedytColors.Focus))),
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
    ) {
        Text(text, style = Type.BodyBold.copy(color = SpeedytColors.Background), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = SpeedytColors.CardHigh,
            contentColor = SpeedytColors.Text,
            focusedContainerColor = SpeedytColors.CardHigh,
            focusedContentColor = SpeedytColors.Text,
            pressedContainerColor = SpeedytColors.CardHigh,
            pressedContentColor = SpeedytColors.Text,
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(2.dp, SpeedytColors.Track)),
            focusedBorder = Border(BorderStroke(4.dp, SpeedytColors.Focus)),
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
    ) {
        Text(text, style = Type.BodyBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

/** A 240 degree dial. [fraction] 0..1 fills the arc; the centre is free for a number. */
@Composable
fun Gauge(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(250), label = "gauge")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.07f
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(
                SpeedytColors.Track, 150f, 240f, false, topLeft, arc,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.005f) {
                drawArc(
                    color, 150f, 240f * animated, false, topLeft, arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
